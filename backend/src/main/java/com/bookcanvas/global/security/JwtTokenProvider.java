package com.bookcanvas.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Access Token 생성 / 파싱 / 검증
 *
 * <p><b>토큰 구조 (payload):</b>
 * <pre>{@code
 * {
 *   "sub": "1",                       // userId
 *   "email": "reader@bookcanvas.com",
 *   "iat": 1760000000,                // 발급 시각
 *   "exp": 1760086400                 // 만료 시각
 * }
 * }</pre>
 * 서명 알고리즘은 비밀 키 길이에 맞춰 HS256~HS512 중 자동 선택됩니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final long validityMillis;

    public JwtTokenProvider(JwtProperties properties) {
        // 32byte 미만이면 Keys.hmacShaKeyFor 가 WeakKeyException 을 던져 서버 시작 단계에서 바로 알 수 있음
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.validityMillis = properties.accessTokenValiditySeconds() * 1000;
    }

    /** 로그인 성공 시 Access Token 발급 */
    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    public long getValiditySeconds() {
        return validityMillis / 1000;
    }

    /**
     * 토큰을 검증하고 사용자 정보를 꺼냅니다.
     *
     * @throws TokenException 서명이 틀렸거나, 형식이 잘못됐거나, 만료된 경우
     */
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthUser(Long.valueOf(claims.getSubject()), claims.get(CLAIM_EMAIL, String.class));

        } catch (ExpiredJwtException e) {
            throw new TokenException(true);
        } catch (JwtException | IllegalArgumentException e) { // IllegalArgumentException: 빈 문자열, subject 가 숫자가 아님 등
            log.debug("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
            throw new TokenException(false);
        }
    }

    /** 토큰 검증 실패. {@code expired} 로 '만료'와 '위조/형식 오류'를 구분합니다. */
    public static class TokenException extends RuntimeException {
        private final boolean expired;

        public TokenException(boolean expired) {
            super(expired ? "만료된 토큰" : "유효하지 않은 토큰");
            this.expired = expired;
        }

        public boolean isExpired() {
            return expired;
        }
    }
}
