package com.bookcanvas.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 {@code jwt.*} 설정값
 *
 * @param secret                     Base64 로 인코딩된 HMAC-SHA 비밀 키 (디코딩 후 32byte 이상)
 * @param accessTokenValiditySeconds Access Token 유효 시간(초)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds
) {}
