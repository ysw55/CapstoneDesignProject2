package com.bookcanvas.domain.user.entity;

/**
 * 가입 / 로그인 방식
 * KAKAO, GOOGLE 은 현재 캡스톤 시연용 간편 로그인으로만 생성됩니다. (실제 OAuth 연동 전)
 */
public enum AuthProvider {
    LOCAL,
    KAKAO,
    GOOGLE
}
