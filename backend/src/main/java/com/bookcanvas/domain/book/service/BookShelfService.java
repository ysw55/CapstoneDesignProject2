package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.ShelfBookRequestDto;
import com.bookcanvas.domain.book.dto.ShelfBookResponseDto;
import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 내 책장 조회 / 추가 / 수정 / 삭제 (독후감 없는 일반 등록)
 *
 * <p>완독 인증은 {@link BookCompletionService} 가 담당하며, 같은 책장 항목에 인증 정보를 더합니다.
 * 인증된 책은 보상 중복 지급을 막기 위해 표지 색만 바꿀 수 있고 삭제할 수 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookShelfService {

    private final BookCompletionRepository bookCompletionRepository;
    private final UserRepository userRepository;
    private final BookFinder bookFinder;

    /** 로그인 사용자의 책장 전체 (직접 정한 순서, 없으면 최근 완독 순) */
    public List<ShelfBookResponseDto> getMyShelf(Long userId) {
        return bookCompletionRepository.findShelfByUserId(userId).stream()
                .map(ShelfBookResponseDto::from)
                .toList();
    }

    @Transactional
    public ShelfBookResponseDto addBook(Long userId, ShelfBookRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Book book = bookFinder.findOrCreate(request.title(), request.author());

        if (bookCompletionRepository.findByUserIdAndBookId(userId, book.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_ON_SHELF);
        }

        BookCompletion entry = BookCompletion.builder()
                .user(user)
                .book(book)
                .completedAt(request.completedAt())
                .coverColor(request.coverColor())
                .build();
        saveAndFlush(entry);

        log.info("[Shelf] 책장 추가 - userId={}, bookId={}", userId, book.getId());
        return ShelfBookResponseDto.from(entry);
    }

    @Transactional
    public ShelfBookResponseDto updateBook(Long userId, Long completionId, ShelfBookRequestDto request) {
        BookCompletion entry = findOwned(userId, completionId);

        if (entry.isCertified()) {
            // 인증한 책: 표지 색만 변경 가능
            boolean infoChanged = !entry.getBook().getTitle().equals(request.title())
                    || !entry.getBook().getAuthor().equals(request.author())
                    || !entry.getCompletedAt().equals(request.completedAt());
            if (infoChanged) {
                throw new BusinessException(ErrorCode.CERTIFIED_BOOK_LOCKED);
            }
        } else {
            Book book = bookFinder.findOrCreate(request.title(), request.author());
            boolean otherEntryExists = !Objects.equals(book.getId(), entry.getBook().getId())
                    && bookCompletionRepository.findByUserIdAndBookId(userId, book.getId()).isPresent();
            if (otherEntryExists) {
                throw new BusinessException(ErrorCode.ALREADY_ON_SHELF);
            }
            entry.changeBook(book, request.completedAt());
        }

        if (request.coverColor() != null) {
            entry.changeCoverColor(request.coverColor());
        }
        saveAndFlush(entry);
        return ShelfBookResponseDto.from(entry);
    }

    /**
     * 책장 순서 저장. 내 책장의 모든 책을 원하는 순서대로 한 번씩 담아 보내야 한다.
     * (일부만 오거나 남의 책·중복이 섞이면 거절 — 화면이 오래된 상태일 수 있음)
     */
    @Transactional
    public void reorder(Long userId, List<Long> completionIds) {
        List<BookCompletion> mine = bookCompletionRepository.findShelfByUserId(userId);
        Map<Long, BookCompletion> byId = mine.stream()
                .collect(Collectors.toMap(BookCompletion::getId, Function.identity()));

        if (completionIds.size() != byId.size() || !byId.keySet().equals(new HashSet<>(completionIds))) {
            throw new BusinessException(ErrorCode.INVALID_SHELF_ORDER);
        }
        for (int i = 0; i < completionIds.size(); i++) {
            byId.get(completionIds.get(i)).moveTo(i);
        }
        log.info("[Shelf] 순서 변경 - userId={}, count={}", userId, completionIds.size());
    }

    @Transactional
    public void deleteBook(Long userId, Long completionId) {
        BookCompletion entry = findOwned(userId, completionId);
        if (entry.isCertified()) {
            throw new BusinessException(ErrorCode.CERTIFIED_BOOK_LOCKED);
        }
        bookCompletionRepository.delete(entry);
        log.info("[Shelf] 책장에서 삭제 - userId={}, completionId={}", userId, completionId);
    }

    // =========================================================================

    /** 남의 책장 항목은 '없는 것'으로 응답 (존재 여부도 노출하지 않음) */
    private BookCompletion findOwned(Long userId, Long completionId) {
        return bookCompletionRepository.findByIdAndUserId(completionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHELF_BOOK_NOT_FOUND));
    }

    private void saveAndFlush(BookCompletion entry) {
        try {
            bookCompletionRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            // 같은 책을 동시에 두 번 추가한 경우 — DB unique 제약이 최종 방어선
            throw new BusinessException(ErrorCode.ALREADY_ON_SHELF);
        }
    }
}
