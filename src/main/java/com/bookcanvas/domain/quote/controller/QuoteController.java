package com.bookcanvas.domain.quote.controller;

import com.bookcanvas.domain.quote.dto.CommentDto;
import com.bookcanvas.domain.quote.dto.QuoteRequestDto;
import com.bookcanvas.domain.quote.dto.QuoteResponse;
import com.bookcanvas.domain.quote.service.QuoteService;
import com.bookcanvas.global.common.dto.PageResponse;
import com.bookcanvas.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 커뮤니티 구절(Quote) API
 *
 * <p><b>Base URL:</b> {@code /api/quotes}
 * <ul>
 *   <li>{@code GET    /}                   피드 조회 (공개, 로그인 시 liked/mine 포함) — {@code ?page=0&size=20&tagIds=1,3}</li>
 *   <li>{@code POST   /}                   글쓰기 (JWT)</li>
 *   <li>{@code PUT    /{id}}               내 글 수정 (JWT)</li>
 *   <li>{@code DELETE /{id}}               내 글 삭제 (JWT)</li>
 *   <li>{@code POST   /{id}/like}          좋아요 (JWT, 멱등)</li>
 *   <li>{@code DELETE /{id}/like}          좋아요 취소 (JWT, 멱등)</li>
 *   <li>{@code GET    /{id}/comments}      댓글 목록 (공개)</li>
 *   <li>{@code POST   /{id}/comments}      댓글 작성 (JWT)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private static final int MAX_PAGE_SIZE = 50;
    /** 정렬은 서버가 고정 — 클라이언트가 임의 필드로 정렬해 오류를 내지 못하게 */
    private static final Sort FEED_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final QuoteService quoteService;

    @GetMapping
    public ResponseEntity<PageResponse<QuoteResponse>> getFeed(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), FEED_SORT);
        return ResponseEntity.ok(quoteService.getFeed(userIdOrNull(authUser), tagIds, pageable));
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody QuoteRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.create(authUser.userId(), request));
    }

    @PutMapping("/{quoteId}")
    public ResponseEntity<QuoteResponse> update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long quoteId,
            @Valid @RequestBody QuoteRequestDto request
    ) {
        return ResponseEntity.ok(quoteService.update(authUser.userId(), quoteId, request));
    }

    @DeleteMapping("/{quoteId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long quoteId) {
        quoteService.delete(authUser.userId(), quoteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{quoteId}/like")
    public ResponseEntity<CommentDto.LikeResponse> like(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long quoteId) {
        return ResponseEntity.ok(quoteService.like(authUser.userId(), quoteId));
    }

    @DeleteMapping("/{quoteId}/like")
    public ResponseEntity<CommentDto.LikeResponse> unlike(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long quoteId) {
        return ResponseEntity.ok(quoteService.unlike(authUser.userId(), quoteId));
    }

    @GetMapping("/{quoteId}/comments")
    public ResponseEntity<List<CommentDto.Response>> getComments(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long quoteId
    ) {
        return ResponseEntity.ok(quoteService.getComments(userIdOrNull(authUser), quoteId));
    }

    @PostMapping("/{quoteId}/comments")
    public ResponseEntity<CommentDto.Response> addComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long quoteId,
            @Valid @RequestBody CommentDto.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.addComment(authUser.userId(), quoteId, request));
    }

    /** 공개 API 는 비로그인도 호출하므로 principal 이 없을 수 있음 */
    private static Long userIdOrNull(AuthUser authUser) {
        return authUser == null ? null : authUser.userId();
    }
}
