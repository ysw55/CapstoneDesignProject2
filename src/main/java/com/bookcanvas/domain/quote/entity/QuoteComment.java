package com.bookcanvas.domain.quote.entity;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구절에 달린 댓글
 */
@Entity
@Table(name = "quote_comment",
        indexes = @Index(name = "idx_quote_comment_quote", columnList = "quote_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_comment_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_comment_quote"))
    private Quote quote;

    @Column(nullable = false, length = 200)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuoteComment(User user, Quote quote, String content) {
        this.user = user;
        this.quote = quote;
        this.content = content;
    }
}
