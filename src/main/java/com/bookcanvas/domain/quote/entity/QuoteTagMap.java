package com.bookcanvas.domain.quote.entity;

import com.bookcanvas.domain.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Quote ↔ Tag N:M 관계를 위한 명시적 중간 매핑 테이블 엔티티
 *
 * <p>JPA의 @ManyToMany 대신 중간 엔티티를 명시적으로 생성하여:
 * <ul>
 *   <li>중간 테이블에 추가 컬럼(예: createdAt) 부여 가능</li>
 *   <li>관계 추가/삭제 시 명확한 생명주기 제어</li>
 *   <li>N+1 문제와 불필요한 전체 컬렉션 로딩 방지</li>
 * </ul>
 */
@Entity
@Table(name = "quote_tag_map",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quote_tag",
                columnNames = {"quote_id", "tag_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteTagMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_tag_map_id")
    private Long id;

    /**
     * 연결된 구절 (N:1)
     * QuoteTagMap 입장에서 Many, Quote 입장에서 One
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_tag_map_quote"))
    private Quote quote;

    /**
     * 연결된 태그 (N:1)
     * QuoteTagMap 입장에서 Many, Tag 입장에서 One
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_tag_map_tag"))
    private Tag tag;

    /** 태그가 이 구절에 붙여진 시각 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public QuoteTagMap(Quote quote, Tag tag) {
        this.quote = quote;
        this.tag = tag;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * Quote와 Tag를 연결하는 매핑 객체를 생성하고,
     * Quote 측 컬렉션에도 자동으로 등록
     */
    public static QuoteTagMap of(Quote quote, Tag tag) {
        QuoteTagMap map = QuoteTagMap.builder()
                .quote(quote)
                .tag(tag)
                .build();
        quote.addQuoteTagMap(map);
        return map;
    }
}
