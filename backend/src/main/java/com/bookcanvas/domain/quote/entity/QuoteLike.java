package com.bookcanvas.domain.quote.entity;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구절 좋아요 — 한 사용자는 한 구절에 한 번만 (uk_quote_like_user_quote)
 */
@Entity
@Table(name = "quote_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_quote_like_user_quote", columnNames = {"user_id", "quote_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_like_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_like_quote"))
    private Quote quote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuoteLike(User user, Quote quote) {
        this.user = user;
        this.quote = quote;
    }
}
