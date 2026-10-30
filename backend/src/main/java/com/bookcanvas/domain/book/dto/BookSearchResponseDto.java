package com.bookcanvas.domain.book.dto;

import java.util.List;

/**
 * 도서 검색 응답
 *
 * @param hasMore 다음 페이지가 더 있는지 ({@code page + 1} 로 이어서 요청)
 */
public record BookSearchResponseDto(List<BookSearchResultDto> results, boolean hasMore) {
}
