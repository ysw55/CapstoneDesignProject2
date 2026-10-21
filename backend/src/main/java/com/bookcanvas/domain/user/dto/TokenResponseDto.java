package com.bookcanvas.domain.user.dto;

/**
 * 로그인 / 회원가입 성공 응답
 *
 * <pre>{@code
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400,
 *   "user": { "userId": 1, "email": "...", "username": "...", ... }
 * }
 * }</pre>
 *
 * @param expiresIn Access Token 유효 시간(초)
 */
public record TokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponseDto user
) {
    public static TokenResponseDto bearer(String accessToken, long expiresIn, UserResponseDto user) {
        return new TokenResponseDto(accessToken, "Bearer", expiresIn, user);
    }
}
