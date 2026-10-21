package com.bookcanvas.domain.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 완독 인증 요청 DTO
 *
 * <p>인증하는 사용자는 요청 본문이 아니라 JWT 에서 꺼냅니다.
 * (본문에 userId 를 받으면 다른 사람 계정으로 인증을 요청할 수 있기 때문)
 *
 * <p>프론트의 책장 데이터에는 서버 bookId 가 없으므로 도서 정보를 직접 받고,
 * 서버가 ISBN 또는 제목+저자로 기존 도서를 찾거나 새로 등록합니다.
 *
 * @param title         도서 제목
 * @param author        저자
 * @param isbn          ISBN-13 (선택, 외부 도서 검색 API 연동 시 사용)
 * @param publisher     출판사 (선택)
 * @param coverImageUrl 표지 이미지 URL (선택, 있으면 표지 색상으로 보상 스킨 생성)
 * @param completedAt   다 읽은 날짜 (선택, 없으면 오늘). 미래 날짜 불가
 * @param reviewText    독후감 (공백 제외 최소 200자)
 * @param coverColor    책장 표지 색 (선택, 책장에 없던 책을 새로 꽂을 때만 사용)
 */
public record BookCompletionRequestDto(

        @NotBlank(message = "책 제목을 입력해 주세요.")
        @Size(max = 300, message = "책 제목은 300자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "저자를 입력해 주세요.")
        @Size(max = 200, message = "저자는 200자 이하로 입력해 주세요.")
        String author,

        @Size(max = 20, message = "ISBN 형식이 올바르지 않아요.")
        String isbn,

        @Size(max = 200, message = "출판사는 200자 이하로 입력해 주세요.")
        String publisher,

        @Size(max = 500, message = "표지 이미지 URL 이 너무 길어요.")
        String coverImageUrl,

        @PastOrPresent(message = "완독 날짜는 오늘 이후로 설정할 수 없어요.")
        LocalDate completedAt,

        @NotBlank(message = "독후감을 입력해 주세요.")
        @Size(min = 200, max = 5000, message = "독후감은 200자 이상 5,000자 이하로 작성해 주세요.")
        String reviewText,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "표지 색 형식이 올바르지 않아요.")
        String coverColor
) {}
