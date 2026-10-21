package com.bookcanvas.domain.user.service;

import com.bookcanvas.domain.user.entity.AuthProvider;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;

import java.util.Locale;

/**
 * 시연용 간편 로그인 계정 (실제 카카오/구글 OAuth 연동 전 캡스톤 발표용)
 * 버튼을 누르면 provider 별로 고정된 계정으로 로그인됩니다.
 */
public enum DemoAccounts {

    KAKAO(AuthProvider.KAKAO, "kakao.demo@bookcanvas.app", "moonreader_"),
    GOOGLE(AuthProvider.GOOGLE, "google.demo@bookcanvas.app", "구글독자");

    private final AuthProvider provider;
    private final String email;
    private final String username;

    DemoAccounts(AuthProvider provider, String email, String username) {
        this.provider = provider;
        this.email = email;
        this.username = username;
    }

    public AuthProvider provider() { return provider; }
    public String email() { return email; }
    public String username() { return username; }

    /** "kakao" / "google" → 계정. 그 외는 400 */
    public static DemoAccounts of(String provider) {
        try {
            return valueOf(provider.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
