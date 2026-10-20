package com.bookcanvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// JWT 로 직접 인증하므로 Spring 기본 인메모리 사용자(로그에 임시 비밀번호 출력)는 만들지 않음
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class BookCanvasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookCanvasApplication.class, args);
    }
}
