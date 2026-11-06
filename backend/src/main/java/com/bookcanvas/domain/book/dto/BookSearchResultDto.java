package com.bookcanvas.domain.book.dto;

/**
 * 도서 검색 결과 한 권 (예스24 Open API)
 *
 * <pre>{@code
 * {
 *   "title": "클린 코드",
 *   "author": "로버트 C. 마틴",
 *   "publisher": "인사이트",
 *   "isbn": "9788966260959",
 *   "coverImageUrl": "https://image.yes24.com/goods/12345678/L",
 *   "spineImageUrl": "https://image.yes24.com/goods/12345678/SIDE/L",
 *   "productUrl": "https://www.yes24.com/product/goods/12345678",
 *   "publishedAt": "2013-12-24"
 * }
 * }</pre>
 *
 * @param isbn          ISBN-13 (없으면 ISBN-10, 둘 다 없으면 null)
 * @param coverImageUrl 앞표지 이미지 URL (등록되지 않은 책이면 null)
 * @param spineImageUrl 책등 이미지 URL (등록되지 않은 책이면 null)
 * @param productUrl    예스24 상품 페이지 — 책 정보가 보이는 화면에서 연결해야 하는 이용 조건
 * @param publishedAt   출간일 {@code yyyy-MM-dd} (없으면 null)
 */
public record BookSearchResultDto(
        String title,
        String author,
        String publisher,
        String isbn,
        String coverImageUrl,
        String spineImageUrl,
        String productUrl,
        String publishedAt
) {
}
