package com.bookcanvas.global.security;

/**
 * JWT 에서 꺼낸 로그인 사용자 정보.
 * 컨트롤러에서 {@code @AuthenticationPrincipal AuthUser authUser} 로 받아 씁니다.
 *
 * <p>DB 를 매 요청마다 조회하지 않도록 토큰에 담긴 값만 사용합니다.
 */
public record AuthUser(Long userId, String email) {}
