package com.bookcanvas.domain.tag.entity;

import com.bookcanvas.domain.quote.entity.QuoteTagMap;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상황 태그 엔티티
 * - 구절(Quote)에 붙이는 감성/상황 태그 (예: #위로, #동기부여, #새벽감성)
 * - Quote와 N:M 관계 → QuoteTagMap 중간 테이블로 매핑
 */
@Entity
@Table(name = "tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    /** 태그 이름 (예: "위로", "동기부여") */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 태그 설명 */
    @Column(length = 200)
    private String description;

    // QuoteTagMap을 통한 역방향 참조
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuoteTagMap> quoteTagMaps = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Tag(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
