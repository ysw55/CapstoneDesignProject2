package com.bookcanvas.domain.tag;

import com.bookcanvas.domain.tag.entity.Tag;
import com.bookcanvas.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 서버 시작 시 기본 상황 태그가 없으면 만들어 둡니다. (이미 있으면 건너뜀 → 여러 번 실행해도 안전)
 *
 * <p>프론트의 {@code TAG_OPTIONS}(mockData.js)와 이름이 같아야 글쓰기가 동작합니다.
 * 이모지는 화면 표시용이라 description 에 함께 보관합니다.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DefaultTagInitializer implements ApplicationRunner {

    public record DefaultTag(String emoji, String name) {}

    public static final List<DefaultTag> DEFAULT_TAGS = List.of(
            new DefaultTag("💧", "위로가 필요할 때"),
            new DefaultTag("🌱", "새로운 시작 앞에서"),
            new DefaultTag("🌙", "마음이 조급할 때"),
            new DefaultTag("🔥", "다시 일어서고 싶을 때"),
            new DefaultTag("✨", "꿈을 응원받고 싶을 때"),
            new DefaultTag("☕", "여유가 필요할 때"),
            new DefaultTag("💌", "사랑이 어려울 때")
    );

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (DefaultTag t : DEFAULT_TAGS) {
            if (tagRepository.findByName(t.name()).isEmpty()) {
                tagRepository.save(Tag.builder().name(t.name()).description(t.emoji()).build());
                created++;
            }
        }
        if (created > 0) log.info("[Init] 기본 상황 태그 {}개 생성", created);
    }
}
