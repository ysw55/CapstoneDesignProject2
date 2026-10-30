package com.bookcanvas.domain.book.dto;

import com.bookcanvas.domain.book.entity.Bookshelf;

/**
 * 책장 하나
 *
 * <pre>{@code
 * { "shelfId": 3, "name": "소설", "position": 1, "bookCount": 7 }
 * }</pre>
 *
 * @param bookCount 이 책장에 꽂힌 책 수 (보관함에 넣은 책은 제외)
 */
public record BookshelfResponseDto(Long shelfId, String name, int position, long bookCount) {

    public static BookshelfResponseDto of(Bookshelf shelf, long bookCount) {
        return new BookshelfResponseDto(shelf.getId(), shelf.getName(), shelf.getPosition(), bookCount);
    }
}
