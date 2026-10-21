package com.bookcanvas.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 시연용 간편 로그인 요청
 *
 * @param provider "kakao" 또는 "google"
 */
public record DemoLoginRequestDto(
        @NotBlank(message = "로그인 방식을 선택해 주세요.")
        String provider
) {}
