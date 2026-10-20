package com.bookcanvas.domain.book.dto;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.user.entity.User;

import java.time.LocalDate;

/**
 * 완독 인증 성공 응답
 *
 * <pre>{@code
 * {
 *   "completionId": 12,
 *   "book": { "bookId": 5, "title": "데미안", "author": "헤르만 헤세", "coverImageUrl": null },
 *   "completedAt": "2026-10-20",
 *   "reviewLength": 248,
 *   "reward": {
 *     "profileBackgroundColor": "#1B4D3E",
 *     "profileBorderColor": "#133A2F",
 *     "earnedPoint": 100,
 *     "totalPoint": 300
 *   }
 * }
 * }</pre>
 */
public record BookCompletionResponseDto(
        Long completionId,
        BookInfo book,
        LocalDate completedAt,
        int reviewLength,
        RewardInfo reward
) {

    public static BookCompletionResponseDto of(BookCompletion completion, User user, int reviewLength) {
        Book book = completion.getBook();
        return new BookCompletionResponseDto(
                completion.getId(),
                new BookInfo(book.getId(), book.getTitle(), book.getAuthor(), book.getCoverImageUrl()),
                completion.getCompletedAt(),
                reviewLength,
                new RewardInfo(
                        user.getProfileBackgroundColor(),
                        user.getProfileBorderColor(),
                        completion.getEarnedPoint(),
                        user.getRewardPoint())
        );
    }

    public record BookInfo(Long bookId, String title, String author, String coverImageUrl) {}

    /**
     * @param earnedPoint 이번 인증으로 받은 포인트
     * @param totalPoint  누적 포인트
     */
    public record RewardInfo(
            String profileBackgroundColor,
            String profileBorderColor,
            int earnedPoint,
            int totalPoint
    ) {}
}
