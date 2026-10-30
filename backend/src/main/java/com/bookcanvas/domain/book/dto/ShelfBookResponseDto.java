package com.bookcanvas.domain.book.dto;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내 책장의 책 한 권
 *
 * <pre>{@code
 * {
 *   "completionId": 12,
 *   "book": { "bookId": 5, "title": "데미안", "author": "헤르만 헤세", "coverImageUrl": null },
 *   "completedAt": "2026-10-12",
 *   "coverColor": "#1B4D3E",
 *   "certified": true,             // 완독 인증(✓ 배지) 여부
 *   "certifiedAt": "2026-10-12T21:03:00",
 *   "earnedPoint": 100,
 *   "shelfOrder": 2,               // 직접 정한 위치. 정한 적 없으면 null
 *   "archived": false,             // 보관함에 있는 책이면 true
 *   "shelfId": 3                   // 꽂혀 있는 책장
 * }
 * }</pre>
 */
public record ShelfBookResponseDto(
        Long completionId,
        BookCompletionResponseDto.BookInfo book,
        LocalDate completedAt,
        String coverColor,
        boolean certified,
        LocalDateTime certifiedAt,
        int earnedPoint,
        Integer shelfOrder,
        boolean archived,
        Long shelfId
) {
    public static ShelfBookResponseDto from(BookCompletion c) {
        Book b = c.getBook();
        return new ShelfBookResponseDto(
                c.getId(),
                new BookCompletionResponseDto.BookInfo(b.getId(), b.getTitle(), b.getAuthor(), b.getCoverImageUrl()),
                c.getCompletedAt(),
                c.getCoverColor(),
                c.isCertified(),
                c.getCertifiedAt(),
                c.getEarnedPoint(),
                c.getShelfOrder(),
                c.isArchived(),
                c.getShelf() == null ? null : c.getShelf().getId()
        );
    }
}
