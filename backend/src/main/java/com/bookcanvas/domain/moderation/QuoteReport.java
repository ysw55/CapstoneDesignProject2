package com.bookcanvas.domain.moderation;

import com.bookcanvas.domain.quote.entity.Quote;
import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 글 신고 — 한 사용자는 한 글을 한 번만 (uk_quote_report_user_quote).
 * 신고한 글은 신고한 사람의 피드에서 숨긴다. 운영자 처리 화면은 아직 없어 DB 에서 확인한다.
 */
@Entity
@Table(name = "quote_report",
        uniqueConstraints = @UniqueConstraint(name = "uk_quote_report_user_quote", columnNames = {"user_id", "quote_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_report_user"))
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quote_report_quote"))
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private ReportReason reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuoteReport(User reporter, Quote quote, ReportReason reason) {
        this.reporter = reporter;
        this.quote = quote;
        this.reason = reason;
    }
}
