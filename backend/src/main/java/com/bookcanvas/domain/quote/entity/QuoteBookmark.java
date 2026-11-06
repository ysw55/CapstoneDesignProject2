package com.bookcanvas.domain.quote.entity;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구절 북마크(저장) — 한 사용자는 한 구절을 한 번만 (uk_quote_bookmark_user_quote).
 * 프로필의 "저장한 문장" 에 최근 저장한 순으로 모아 보인다.
 */
@Entity
@Table(name = "quote_bookmark",
        uniqueConstraints = @UniqueConstraint(name = "uk_quote_bookmark_user_quote", columnNames = {"user_id", "quote_id"}),
        indexes = @Index(name = "idx_quote_bookmark_user_created", columnList = "user_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_bookmark_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_bookmark_quote"))
    private Quote quote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuoteBookmark(User user, Quote quote) {
        this.user = user;
        this.quote = quote;
    }
}
