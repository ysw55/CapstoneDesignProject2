package com.bookcanvas.domain.book.dto;

import com.bookcanvas.domain.book.entity.Bookshelf;
import jakarta.validation.constraints.Size;

/**
 * 책장 만들기 / 이름 바꾸기 요청. 만들 때 이름을 비우면 "책장 N" 으로 정해진다.
 */
public record BookshelfRequestDto(
        @Size(max = Bookshelf.MAX_NAME_LENGTH, message = "책장 이름은 20자 이하로 입력해 주세요.")
        String name
) {
    public BookshelfRequestDto {
        name = name == null ? null : name.trim();
    }
}
