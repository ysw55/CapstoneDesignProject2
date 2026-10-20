package com.bookcanvas.global.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 서버 시작 직후, 필수 비밀 값(DB 비밀번호, JWT 키)이 설정됐는지 확인합니다.
 *
 * <p>설정이 없으면 Spring 은 {@code ${DB_PASSWORD}} 문자열을 그대로 비밀번호로 써서
 * 'Access denied' 같은 엉뚱한 오류를 내기 때문에, 그 전에 무엇을 하면 되는지 알려 주고 멈춥니다.
 * (application.yml 의 config import 처리가 끝난 뒤 실행되도록 가장 마지막 순서로 등록)
 */
public class RequiredSecretsValidator implements EnvironmentPostProcessor, Ordered {

    /** 프로퍼티 키 → 사람이 설정할 이름 */
    private static final Map<String, String> REQUIRED = Map.of(
            "spring.datasource.password", "DB_PASSWORD",
            "jwt.secret", "JWT_SECRET"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missing = new ArrayList<>();
        REQUIRED.forEach((key, name) -> {
            try {
                if (environment.getProperty(key) == null) missing.add(name);
            } catch (IllegalArgumentException unresolvedPlaceholder) {
                missing.add(name);
            }
        });

        if (!missing.isEmpty()) {
            throw new IllegalStateException("""

                    ──────────────────────────────────────────────────────────────
                    필수 설정이 없습니다: %s
                    프로젝트 루트에서 application-local.example.yml 을 application-local.yml 로 복사하고
                    값을 채운 뒤, 프로젝트 루트에서 다시 실행하세요. (또는 같은 이름의 환경 변수로 지정)
                    ──────────────────────────────────────────────────────────────"""
                    .formatted(String.join(", ", missing.stream().sorted().toList())));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
