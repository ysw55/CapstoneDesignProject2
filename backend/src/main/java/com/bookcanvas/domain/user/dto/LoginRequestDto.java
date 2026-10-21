package com.bookcanvas.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청
 */
public record LoginRequestDto(

        @NotBlank(message = "이메일을 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
    public LoginRequestDto {
        email = email == null ? null : email.trim();
    }
}
