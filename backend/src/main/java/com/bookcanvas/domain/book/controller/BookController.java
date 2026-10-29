package com.bookcanvas.domain.book.controller;

import com.bookcanvas.domain.book.dto.BookCompletionRequestDto;
import com.bookcanvas.domain.book.dto.BookCompletionResponseDto;
import com.bookcanvas.domain.book.dto.ShelfBookRequestDto;
import com.bookcanvas.domain.book.dto.ShelfBookResponseDto;
import com.bookcanvas.domain.book.dto.ShelfOrderRequestDto;
import com.bookcanvas.domain.book.service.BookCompletionService;
import com.bookcanvas.domain.book.service.BookShelfService;
import com.bookcanvas.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 도서 · 내 책장 API (모두 JWT 필수)
 *
 * <p><b>Base URL:</b> {@code /api/books}
 * <ul>
 *   <li>{@code GET    /completions}       내 책장(완독 기록) 전체 — {@code certified} 로 ✓ 배지 표시</li>
 *   <li>{@code POST   /completions}       책장에 추가 (독후감 없이, 보상 없음)</li>
 *   <li>{@code PUT    /completions/order} 책장 순서 저장 (모든 책의 id 를 원하는 순서대로)</li>
 *   <li>{@code PUT    /completions/{id}}  책 정보 수정 (인증한 책은 표지 색만)</li>
 *   <li>{@code DELETE /completions/{id}}  책장에서 삭제 (인증한 책은 불가)</li>
 *   <li>{@code POST   /completion}        완독 인증 (독후감 200자 이상, 보상 지급)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookCompletionService bookCompletionService;
    private final BookShelfService bookShelfService;

    @GetMapping("/completions")
    public ResponseEntity<List<ShelfBookResponseDto>> getMyShelf(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(bookShelfService.getMyShelf(authUser.userId()));
    }

    @PostMapping("/completions")
    public ResponseEntity<ShelfBookResponseDto> addToShelf(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ShelfBookRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookShelfService.addBook(authUser.userId(), request));
    }

    @PutMapping("/completions/order")
    public ResponseEntity<Void> reorderShelf(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ShelfOrderRequestDto request
    ) {
        bookShelfService.reorder(authUser.userId(), request.completionIds());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/completions/{completionId}")
    public ResponseEntity<ShelfBookResponseDto> updateShelfBook(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long completionId,
            @Valid @RequestBody ShelfBookRequestDto request
    ) {
        return ResponseEntity.ok(bookShelfService.updateBook(authUser.userId(), completionId, request));
    }

    @DeleteMapping("/completions/{completionId}")
    public ResponseEntity<Void> deleteShelfBook(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long completionId
    ) {
        bookShelfService.deleteBook(authUser.userId(), completionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 완독 인증
     *
     * <p><b>요청:</b>
     * <pre>{@code
     * POST /api/books/completion
     * Authorization: Bearer <token>
     * {
     *   "title": "데미안",
     *   "author": "헤르만 헤세",
     *   "completedAt": "2026-10-20",
     *   "reviewText": "200자 이상의 독후감..."
     * }
     * }</pre>
     *
     * <p><b>응답:</b> 201 Created + {@link BookCompletionResponseDto}
     * <ul>
     *   <li>400 INVALID_INPUT / REVIEW_TOO_SHORT — 독후감 200자 미만 등</li>
     *   <li>401 UNAUTHORIZED / EXPIRED_TOKEN — 토큰 없음·만료</li>
     *   <li>409 ALREADY_COMPLETED — 이미 인증한 책</li>
     * </ul>
     */
    @PostMapping("/completion")
    public ResponseEntity<BookCompletionResponseDto> certifyCompletion(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BookCompletionRequestDto request
    ) {
        BookCompletionResponseDto response = bookCompletionService.certifyCompletion(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
