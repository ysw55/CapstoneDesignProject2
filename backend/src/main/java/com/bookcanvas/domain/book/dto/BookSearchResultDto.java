package com.bookcanvas.domain.book.dto;

/**
 * 도서 검색 결과 한 권 (카카오 책 검색)
 *
 * <pre>{@code
 * {
 *   "title": "데미안",
 *   "author": "헤르만 헤세",
 *   "publisher": "민음사",
 *   "isbn": "9788937460777",
 *   "coverImageUrl": "https://search1.kakaocdn.net/...",
 *   "publishedAt": "2000-12-20"
 * }
 * }</pre>
 *
 * @param isbn          ISBN-13 (없으면 ISBN-10, 둘 다 없으면 null)
 * @param coverImageUrl 표지 이미지 URL (없으면 null)
 * @param publishedAt   출간일 {@code yyyy-MM-dd} (없으면 null)
 */
public record BookSearchResultDto(
        String title,
        String author,
        String publisher,
        String isbn,
        String coverImageUrl,
        String publishedAt
) {
}
