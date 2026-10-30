package com.bookcanvas.domain.book.controller;

import com.bookcanvas.domain.book.dto.BookSearchResponseDto;
import com.bookcanvas.domain.book.service.BookSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 도서 검색 API (JWT 필수)
 *
 * <p>{@code GET /api/books/search?q=데미안&page=1} — 카카오 책 검색 결과 10권씩.
 * 검색을 쓸 수 없으면 503 {@code BOOK_SEARCH_UNAVAILABLE}, 검색어가 비어 있으면 빈 목록.
 */
@RestController
@RequestMapping("/api/books/search")
@RequiredArgsConstructor
public class BookSearchController {

    private final BookSearchService bookSearchService;

    @GetMapping
    public ResponseEntity<BookSearchResponseDto> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(bookSearchService.search(query, page));
    }
}
