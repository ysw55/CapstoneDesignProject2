package com.bookcanvas.domain.user.dto;

import com.bookcanvas.domain.user.entity.AuthProvider;
import com.bookcanvas.domain.user.entity.User;

/**
 * 로그인한 사용자 정보 (비밀번호 등 민감 정보 제외)
 *
 * @param provider           가입 방식 (LOCAL / KAKAO / GOOGLE) — 소셜 계정이면 비밀번호 변경 메뉴를 숨김
 * @param completedBookCount 완독 인증한 책 수
 */
public record UserResponseDto(
        Long userId,
        String email,
        String username,
        String bio,
        AuthProvider provider,
        String profileBackgroundColor,
        String profileBorderColor,
        int rewardPoint,
        long completedBookCount
) {
    public static UserResponseDto of(User user, long completedBookCount) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProvider(),
                user.getProfileBackgroundColor(),
                user.getProfileBorderColor(),
                user.getRewardPoint(),
                completedBookCount
        );
    }
}
