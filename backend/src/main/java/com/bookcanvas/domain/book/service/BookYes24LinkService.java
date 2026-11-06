package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookSearchResultDto;
import com.bookcanvas.domain.book.dto.Yes24LinkResultDto;
import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.book.repository.BookRepository;
import com.bookcanvas.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 이미 책장에 있는 책을 예스24 책 정보(표지 · 책등 · 상품 링크)로 한꺼번에 연결한다.
 *
 * <p><b>찾는 방법:</b> ISBN 이 있으면 ISBN 으로 정확히 찾고, 없거나 못 찾으면 제목으로 검색해서
 * <em>제목이 같거나 그 제목으로 시작하고 저자도 겹치는</em> 책만 같은 책으로 본다.
 * 엉뚱한 책에 연결하는 것보다 못 찾는 쪽이 안전하므로 애매하면 연결하지 않는다.
 *
 * <p><b>호출 보호:</b> 한 번에 {@value #MAX_BOOKS_PER_RUN}권까지, 호출 사이에 간격을 두고,
 * 키 오류·호출 한도·장애를 만나면 거기서 멈춘다 (남은 책은 다시 시도하면 이어서 처리).
 * 예스24 호출은 DB 트랜잭션 밖에서 해서 느린 외부 호출이 DB 연결을 붙잡지 않게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookYes24LinkService {

    static final int MAX_BOOKS_PER_RUN = 30;
    private static final long PAUSE_BETWEEN_CALLS_MS = 250;

    private final BookCompletionRepository bookCompletionRepository;
    private final BookRepository bookRepository;
    private final BookSearchService bookSearchService;
    private final TransactionTemplate transactionTemplate;

    /** 호출 간격 (테스트에서는 기다리지 않도록 바꿀 수 있다) */
    long pauseMs = PAUSE_BETWEEN_CALLS_MS;

    public Yes24LinkResultDto linkMyBooks(Long userId) {
        // 1) 연결할 책 모으기 (읽기)
        List<Target> pending = transactionTemplate.execute(status -> {
            Map<Long, Target> targets = new LinkedHashMap<>();
            for (BookCompletion c : bookCompletionRepository.findShelfByUserId(userId)) {
                Book book = c.getBook();
                if (book.isYes24Pending()) {
                    targets.putIfAbsent(book.getId(), new Target(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn()));
                }
            }
            return new ArrayList<>(targets.values());
        });
        if (pending == null || pending.isEmpty()) return new Yes24LinkResultDto(0, 0, 0, false);

        List<Target> batch = pending.subList(0, Math.min(MAX_BOOKS_PER_RUN, pending.size()));

        // 2) 예스24 에서 찾기 (트랜잭션 밖 — 외부 호출)
        Map<Long, Optional<BookSearchResultDto>> found = new LinkedHashMap<>();
        boolean stopped = false;
        for (Target t : batch) {
            try {
                found.put(t.bookId, findMatch(t));
            } catch (BusinessException e) {
                stopped = true; // 키 오류·호출 한도·장애 — 남은 책은 다음에
                break;
            }
            pause();
        }

        // 3) 결과 반영 (쓰기)
        int[] counts = new int[2]; // [연결, 못 찾음]
        transactionTemplate.executeWithoutResult(status -> found.forEach((bookId, match) -> {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) return;
            if (match.isPresent()) {
                BookSearchResultDto m = match.get();
                // ISBN 은 다른 책이 이미 쓰고 있으면 넣지 않는다 (ISBN 은 책마다 하나여야 한다)
                String isbn = m.isbn() != null && book.getIsbn() == null && bookRepository.findByIsbn(m.isbn()).isEmpty() ? m.isbn() : null;
                book.linkYes24(isbn, m.coverImageUrl(), m.spineImageUrl(), m.productUrl());
                counts[0]++;
            } else {
                book.markYes24NotFound();
                counts[1]++;
            }
        }));

        int remaining = pending.size() - found.size();
        log.info("[Yes24Link] userId={}, 연결 {}권, 못 찾음 {}권, 남음 {}권, 중단 {}", userId, counts[0], counts[1], remaining, stopped);
        return new Yes24LinkResultDto(counts[0], counts[1], remaining, stopped);
    }

    private Optional<BookSearchResultDto> findMatch(Target t) {
        if (t.isbn != null) {
            Optional<BookSearchResultDto> byIsbn = bookSearchService.search(t.isbn, 1).results().stream()
                    .filter(r -> t.isbn.equals(r.isbn()))
                    .findFirst();
            if (byIsbn.isPresent()) return byIsbn;
            pause();
        }
        List<BookSearchResultDto> candidates = bookSearchService.search(t.title, 1).results().stream()
                .filter(r -> sameBook(t.title, t.author, r))
                .toList();
        // 책등 이미지가 있는 판본을 먼저 (같은 책의 여러 판본 중에서)
        return candidates.stream().filter(r -> r.spineImageUrl() != null).findFirst()
                .or(() -> candidates.stream().findFirst());
    }

    /** 제목이 같거나 그 제목으로 시작하고(예: "모모" → "모모 MOMO"), 저자가 겹치면 같은 책으로 본다 */
    static boolean sameBook(String title, String author, BookSearchResultDto r) {
        String t = normalize(title);
        String rt = normalize(r.title());
        if (t.isEmpty() || !(rt.equals(t) || rt.startsWith(t))) return false;

        String a = normalize(author);
        String ra = normalize(r.author());
        return !a.isEmpty() && !ra.isEmpty() && (ra.contains(a) || a.contains(ra));
    }

    /** 공백·기호를 없애고 소문자로 — 글자(한글·영문·숫자)만 남긴다 */
    static String normalize(String s) {
        return s == null ? "" : s.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    private void pause() {
        if (pauseMs <= 0) return;
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Target(Long bookId, String title, String author, String isbn) {}
}
