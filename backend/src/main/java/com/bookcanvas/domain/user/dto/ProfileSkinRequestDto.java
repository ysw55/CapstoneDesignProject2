package com.bookcanvas.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 꾸미기 요청 — 배경색 (#RRGGBB). null 이면 해당 스킨 해제.
 * profileImage 는 data URL(JPEG/PNG/WebP), null 이면 사진 삭제.
 */
public record ProfileSkinRequestDto(

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "배경색은 #RRGGBB 형식이어야 해요.")
        String profileBackgroundColor,

        @Size(max = 300_000, message = "프로필 사진이 너무 커요.")
        @Pattern(regexp = "^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$", message = "지원하지 않는 이미지 형식이에요.")
        String profileImage,

        @Pattern(regexp = "^[A-Z0-9_]{1,30}$", message = "테두리 값이 올바르지 않아요.")
        String profileFrame
) {}
