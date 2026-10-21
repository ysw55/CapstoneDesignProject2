package com.bookcanvas.domain.quote.dto;

import com.bookcanvas.domain.quote.entity.Quote;
import com.bookcanvas.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 커뮤니티 피드의 구절(글) 한 개
 *
 * <p>엔티티를 직접 응답으로 내보내면 순환 참조, 불필요한 필드 노출, API 계약 불안정 등의
 * 문제가 발생합니다. DTO를 통해 클라이언트에 필요한 데이터만 선별하여 반환합니다.
 *
 * @param quoteId      구절 PK
 * @param content      발췌 구절 원문
 * @param author       작성자 (닉네임 + 완독 보상 스킨 색)
 * @param book         도서 정보
 * @param tags         상황 태그 목록
 * @param likeCount    좋아요 수
 * @param commentCount 댓글 수
 * @param liked        로그인 사용자가 좋아요를 눌렀는지 (비로그인이면 false)
 * @param mine         로그인 사용자가 쓴 글인지 → 수정/삭제 버튼 표시
 * @param edited       작성 후 수정됐는지
 * @param createdAt    등록 시각
 */
public record QuoteResponse(
        Long quoteId,
        String content,
        AuthorInfo author,
        BookInfo book,
        List<TagInfo> tags,
        long likeCount,
        long commentCount,
        boolean liked,
        boolean mine,
        boolean edited,
        LocalDateTime createdAt
) {

    /**
     * quoteTagMaps 컬렉션 접근 시 Lazy 로딩이 발생합니다.
     * Quote 엔티티의 {@code @BatchSize(size=100)} 덕분에 N건을 처리해도 IN 쿼리로 일괄 로딩됩니다.
     */
    public static QuoteResponse of(Quote quote, long likeCount, long commentCount, boolean liked, Long currentUserId) {
        return new QuoteResponse(
                quote.getId(),
                quote.getContent(),
                AuthorInfo.from(quote.getUser()),
                BookInfo.from(quote),
                TagInfo.fromQuote(quote),
                likeCount,
                commentCount,
                liked,
                Objects.equals(quote.getUser().getId(), currentUserId),
                quote.getEditedAt() != null,
                quote.getCreatedAt()
        );
    }

    /** 작성자 요약 정보 */
    public record AuthorInfo(Long userId, String username, String profileBackgroundColor, String profileBorderColor) {
        public static AuthorInfo from(User user) {
            return new AuthorInfo(user.getId(), user.getUsername(), user.getProfileBackgroundColor(), user.getProfileBorderColor());
        }
    }

    /** 도서 요약 정보 */
    public record BookInfo(Long bookId, String title, String author, String coverImageUrl) {
        static BookInfo from(Quote quote) {
            return new BookInfo(
                    quote.getBook().getId(),
                    quote.getBook().getTitle(),
                    quote.getBook().getAuthor(),
                    quote.getBook().getCoverImageUrl()
            );
        }
    }

    /** 태그 요약 정보 */
    public record TagInfo(Long tagId, String name) {
        static List<TagInfo> fromQuote(Quote quote) {
            return quote.getQuoteTagMaps().stream()
                    .map(qtm -> new TagInfo(qtm.getTag().getId(), qtm.getTag().getName()))
                    .toList();
        }
    }
}
