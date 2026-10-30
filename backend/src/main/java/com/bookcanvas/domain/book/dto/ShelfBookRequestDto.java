package com.bookcanvas.domain.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 내 책장에 책 추가 / 수정 요청 (독후감 없이 책장에만 꽂기)
 *
 * @param coverColor 책장 표지 색 (#RRGGBB, 선택)
 * @param isbn       도서 검색으로 고른 책의 ISBN (선택)
 * @param publisher  출판사 (선택)
 * @param coverImageUrl 표지 이미지 URL (선택, 카카오 이미지 서버 주소만 받는다)
 * @param shelfId    추가할 책장 (선택, 비우면 첫 번째 책장). 수정할 때는 무시된다 — 옮기기는 별도 API
 */
public record ShelfBookRequestDto(

        @NotBlank(message = "책 제목을 입력해 주세요.")
        @Size(max = 300, message = "책 제목은 300자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "저자를 입력해 주세요.")
        @Size(max = 200, message = "저자는 200자 이하로 입력해 주세요.")
        String author,

        @NotNull(message = "다 읽은 날짜를 입력해 주세요.")
        @PastOrPresent(message = "완독 날짜는 오늘 이후로 설정할 수 없어요.")
        LocalDate completedAt,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "표지 색 형식이 올바르지 않아요.")
        String coverColor,

        Long shelfId,

        @Size(max = 20, message = "ISBN 형식이 올바르지 않아요.")
        String isbn,

        @Size(max = 200, message = "출판사는 200자 이하로 입력해 주세요.")
        String publisher,

        @Size(max = 1000, message = "표지 주소가 너무 길어요.")
        String coverImageUrl
) {
    public ShelfBookRequestDto {
        title = title == null ? null : title.trim();
        author = author == null ? null : author.trim();
    }
}
