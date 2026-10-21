package com.bookcanvas.domain.quote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 커뮤니티 글쓰기 / 수정 요청
 *
 * @param bookTitle 책 제목 (책장에서 고르거나 직접 입력)
 * @param author    저자
 * @param content   인상 깊은 문장
 * @param tagName   상황 태그 이름 (예: "위로가 필요할 때") — DB 의 tag.name 과 일치해야 함
 */
public record QuoteRequestDto(

        @NotBlank(message = "책 제목을 입력해 주세요.")
        @Size(max = 300, message = "책 제목은 300자 이하로 입력해 주세요.")
        String bookTitle,

        @NotBlank(message = "저자를 입력해 주세요.")
        @Size(max = 200, message = "저자는 200자 이하로 입력해 주세요.")
        String author,

        @NotBlank(message = "문장을 입력해 주세요.")
        @Size(max = 300, message = "문장은 300자 이하로 입력해 주세요.")
        String content,

        @NotBlank(message = "상황 태그를 골라 주세요.")
        String tagName
) {
    public QuoteRequestDto {
        bookTitle = bookTitle == null ? null : bookTitle.trim();
        author = author == null ? null : author.trim();
        content = content == null ? null : content.trim();
        tagName = tagName == null ? null : tagName.trim();
    }
}
