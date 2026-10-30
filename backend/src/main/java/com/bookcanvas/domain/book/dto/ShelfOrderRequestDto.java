package com.bookcanvas.domain.book.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 책장 순서 저장 요청 — 그 책장에 꽂힌 모든 completionId 를 원하는 순서(앞 → 뒤)로
 *
 * <pre>{@code
 * { "shelfId": 3, "completionIds": [12, 7, 9, 3] }
 * }</pre>
 *
 * @param shelfId 순서를 정할 책장 (선택). 비우면 보관하지 않은 모든 책이 대상
 */
public record ShelfOrderRequestDto(Long shelfId, @NotNull List<@NotNull Long> completionIds) {
}
