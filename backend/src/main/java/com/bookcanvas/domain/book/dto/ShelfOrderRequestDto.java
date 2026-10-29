package com.bookcanvas.domain.book.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 책장 순서 저장 요청 — 내 책장의 모든 completionId 를 원하는 순서(앞 → 뒤)로
 *
 * <pre>{@code
 * { "completionIds": [12, 7, 9, 3] }
 * }</pre>
 */
public record ShelfOrderRequestDto(@NotNull List<@NotNull Long> completionIds) {
}
