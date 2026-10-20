package com.bookcanvas.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 엔티티의 {@code @CreatedDate}, {@code @LastModifiedDate} 자동 기록 활성화.
 *
 * <p>메인 클래스가 아닌 별도 설정으로 분리해 두면 {@code @WebMvcTest} 같은
 * 슬라이스 테스트에서 JPA 관련 빈이 없어 실패하는 문제를 피할 수 있습니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
