package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookshelfRequestDto;
import com.bookcanvas.domain.book.dto.BookshelfResponseDto;
import com.bookcanvas.domain.book.dto.ShelfBookResponseDto;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.entity.Bookshelf;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.book.repository.BookshelfRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 책장 여러 개 관리 (책 한 권을 다루는 {@link BookShelfService} 와는 별개) (만들기 / 이름 바꾸기 / 삭제 / 책 옮기기)
 *
 * <p>사용자는 최대 {@link Bookshelf#MAX_PER_USER}개의 책장을 만들 수 있고, 책 한 권은 책장 하나에만 꽂힌다.
 * 첫 번째 책장은 항상 존재하며 지울 수 없다. 책장을 지우면 거기 있던 책은 첫 번째 책장으로 옮겨진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookshelvesService {

    private final BookshelfRepository bookshelfRepository;
    private final BookCompletionRepository bookCompletionRepository;
    private final UserRepository userRepository;

    public List<BookshelfResponseDto> list(Long userId) {
        ensureDefaultShelf(userId);
        return toDtos(userId);
    }

    /**
     * 첫 번째 책장이 없으면 만들고, 책장이 정해지지 않은 책(책장 기능 이전 데이터, 완독 인증으로 새로 생긴 책)을
     * 그 책장에 꽂는다. 첫 번째 책장을 돌려준다.
     *
     * <p>같은 사용자의 요청이 동시에 와서 기본 책장이 두 개 생기지 않도록 사용자 행을 잠근다.
     */
    public Bookshelf ensureDefaultShelf(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<Bookshelf> shelves = bookshelfRepository.findByUserIdOrderByPositionAscIdAsc(userId);

        Bookshelf first = shelves.isEmpty()
                ? bookshelfRepository.save(Bookshelf.builder().user(user).name("책장 1").position(0).build())
                : shelves.get(0);

        for (BookCompletion c : bookCompletionRepository.findByUserIdAndShelfIsNull(userId)) {
            c.assignShelf(first);
        }
        return first;
    }

    public BookshelfResponseDto create(Long userId, BookshelfRequestDto request) {
        ensureDefaultShelf(userId);
        List<Bookshelf> shelves = bookshelfRepository.findByUserIdOrderByPositionAscIdAsc(userId);
        if (shelves.size() >= Bookshelf.MAX_PER_USER) {
            throw new BusinessException(ErrorCode.BOOKSHELF_LIMIT_REACHED);
        }

        User user = userRepository.getReferenceById(userId);
        int position = shelves.get(shelves.size() - 1).getPosition() + 1;
        String name = request.name() == null || request.name().isBlank() ? "책장 " + (shelves.size() + 1) : request.name();
        Bookshelf shelf = bookshelfRepository.save(Bookshelf.builder().user(user).name(name).position(position).build());

        log.info("[Bookshelf] 책장 만들기 - userId={}, shelfId={}, name={}", userId, shelf.getId(), name);
        return BookshelfResponseDto.of(shelf, 0);
    }

    public BookshelfResponseDto rename(Long userId, Long shelfId, BookshelfRequestDto request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.BOOKSHELF_NAME_REQUIRED);
        }
        Bookshelf shelf = findOwned(userId, shelfId);
        shelf.rename(request.name());
        return toDtos(userId).stream().filter(d -> d.shelfId().equals(shelfId)).findFirst().orElseThrow();
    }

    /** 책장 삭제 — 꽂혀 있던 책(보관함의 책 포함)은 첫 번째 남는 책장으로 옮겨진다 */
    public void delete(Long userId, Long shelfId) {
        ensureDefaultShelf(userId);
        List<Bookshelf> shelves = bookshelfRepository.findByUserIdOrderByPositionAscIdAsc(userId);
        Bookshelf target = findOwned(userId, shelfId);
        if (shelves.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_BOOKSHELF);
        }

        Bookshelf destination = shelves.stream().filter(s -> !s.getId().equals(shelfId)).findFirst().orElseThrow();
        for (BookCompletion c : bookCompletionRepository.findByShelfId(shelfId)) {
            c.moveToShelf(destination);
        }
        bookCompletionRepository.flush();
        bookshelfRepository.delete(target);
        log.info("[Bookshelf] 책장 삭제 - userId={}, shelfId={}, 책 이동 -> shelfId={}", userId, shelfId, destination.getId());
    }

    /** 책을 다른 책장으로 옮긴다 (그 책장의 맨 앞에 놓인다) */
    public ShelfBookResponseDto moveBook(Long userId, Long completionId, Long shelfId) {
        BookCompletion entry = bookCompletionRepository.findByIdAndUserId(completionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHELF_BOOK_NOT_FOUND));
        entry.moveToShelf(findOwned(userId, shelfId));
        return ShelfBookResponseDto.from(entry);
    }

    /** 남의 책장은 '없는 것'으로 응답 */
    public Bookshelf findOwned(Long userId, Long shelfId) {
        return bookshelfRepository.findByIdAndUserId(shelfId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKSHELF_NOT_FOUND));
    }

    private List<BookshelfResponseDto> toDtos(Long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : bookCompletionRepository.countByShelf(userId)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return bookshelfRepository.findByUserIdOrderByPositionAscIdAsc(userId).stream()
                .map(s -> BookshelfResponseDto.of(s, counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }
}
