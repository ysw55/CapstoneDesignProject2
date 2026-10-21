package com.bookcanvas.domain.quote.dto;

import com.bookcanvas.domain.quote.entity.QuoteComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 댓글 요청 / 응답 DTO 모음
 */
public final class CommentDto {

    private CommentDto() {}

    public record Request(
            @NotBlank(message = "댓글을 입력해 주세요.")
            @Size(max = 200, message = "댓글은 200자 이하로 입력해 주세요.")
            String content
    ) {
        public Request {
            content = content == null ? null : content.trim();
        }
    }

    /**
     * @param mine 로그인 사용자가 쓴 댓글인지
     */
    public record Response(
            Long commentId,
            String content,
            QuoteResponse.AuthorInfo author,
            boolean mine,
            LocalDateTime createdAt
    ) {
        public static Response of(QuoteComment c, Long currentUserId) {
            return new Response(
                    c.getId(),
                    c.getContent(),
                    QuoteResponse.AuthorInfo.from(c.getUser()),
                    c.getUser().getId().equals(currentUserId),
                    c.getCreatedAt());
        }
    }

    /** 좋아요 토글 결과 */
    public record LikeResponse(Long quoteId, boolean liked, long likeCount) {}
}
