package com.bookcanvas.domain.book.controller;

import com.bookcanvas.domain.book.dto.BookshelfRequestDto;
import com.bookcanvas.domain.book.dto.BookshelfResponseDto;
import com.bookcanvas.domain.book.service.BookshelvesService;
import com.bookcanvas.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 책장(여러 개) API (모두 JWT 필수)
 *
 * <p><b>Base URL:</b> {@code /api/books/shelves}
 * <ul>
 *   <li>{@code GET    /}       내 책장 목록 (탭 순서, 책 수 포함)</li>
 *   <li>{@code POST   /}       책장 만들기 (최대 5개, 이름을 비우면 "책장 N")</li>
 *   <li>{@code PUT    /{id}}   이름 바꾸기</li>
 *   <li>{@code DELETE /{id}}   삭제 (책은 첫 번째 책장으로 이동, 마지막 책장은 불가)</li>
 * </ul>
 * 책을 다른 책장으로 옮기는 API 는 {@code PUT /api/books/completions/{id}/shelf} 입니다.
 */
@RestController
@RequestMapping("/api/books/shelves")
@RequiredArgsConstructor
public class BookshelfController {

    private final BookshelvesService bookshelvesService;

    @GetMapping
    public ResponseEntity<List<BookshelfResponseDto>> list(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(bookshelvesService.list(authUser.userId()));
    }

    @PostMapping
    public ResponseEntity<BookshelfResponseDto> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BookshelfRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookshelvesService.create(authUser.userId(), request));
    }

    @PutMapping("/{shelfId}")
    public ResponseEntity<BookshelfResponseDto> rename(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long shelfId,
            @Valid @RequestBody BookshelfRequestDto request
    ) {
        return ResponseEntity.ok(bookshelvesService.rename(authUser.userId(), shelfId, request));
    }

    @DeleteMapping("/{shelfId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long shelfId) {
        bookshelvesService.delete(authUser.userId(), shelfId);
        return ResponseEntity.noContent().build();
    }
}
