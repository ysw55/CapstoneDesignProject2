package com.bookcanvas.domain.book.dto;

import jakarta.validation.constraints.NotNull;

/** 책을 다른 책장으로 옮기기 요청 */
public record ShelfMoveRequestDto(@NotNull Long shelfId) {
}
