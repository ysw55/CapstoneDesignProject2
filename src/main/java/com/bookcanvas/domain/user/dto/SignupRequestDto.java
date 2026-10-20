package com.bookcanvas.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청
 *
 * @param email    로그인 ID 로 쓰는 이메일
 * @param password 8~64자, 영문과 숫자를 각각 1자 이상 포함
 * @param username 커뮤니티에 표시되는 닉네임 (2~20자)
 */
public record SignupRequestDto(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 해요.")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력해 주세요.")
        String username
) {
    /** 모바일 키보드 자동 공백 등으로 검증에 실패하지 않도록, 검증 전에 앞뒤 공백 제거 */
    public SignupRequestDto {
        email = email == null ? null : email.trim();
        username = username == null ? null : username.trim();
    }
}
