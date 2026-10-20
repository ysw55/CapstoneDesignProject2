package com.bookcanvas.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 캡스톤 시연용 설정 ({@code app.demo.*})
 *
 * @param seedEnabled  DB 가 비어 있을 때 샘플 데이터 자동 입력
 * @param loginEnabled 카카오/구글 시연용 간편 로그인 허용 (실제 OAuth 아님 — 실서비스에서는 false)
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(boolean seedEnabled, boolean loginEnabled) {}
