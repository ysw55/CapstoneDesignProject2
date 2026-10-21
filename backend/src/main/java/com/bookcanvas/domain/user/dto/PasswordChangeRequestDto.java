package com.bookcanvas.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청
 *
 * @param currentPassword 본인 확인용 현재 비밀번호
 * @param newPassword     새 비밀번호 (회원가입과 같은 규칙: 8~64자, 영문+숫자)
 */
public record PasswordChangeRequestDto(

        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 해요.")
        String newPassword
) {}
