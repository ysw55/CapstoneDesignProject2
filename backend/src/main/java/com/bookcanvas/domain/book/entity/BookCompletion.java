package com.bookcanvas.domain.book.entity;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내 책장의 책 한 권 (= 사용자의 완독 기록)
 *
 * <ul>
 *   <li>책장에 추가만 한 상태: {@code reviewText == null}, 보상 없음</li>
 *   <li>완독 인증한 상태: 200자 이상 독후감 + {@code certifiedAt}, 포인트 지급. 이후 삭제·책 정보 변경 불가</li>
 * </ul>
 * 같은 책은 한 사용자당 한 번만 꽂을 수 있다 (uk_completion_user_book)
 */
@Entity
@Table(name = "book_completion",
        uniqueConstraints = @UniqueConstraint(name = "uk_completion_user_book", columnNames = {"user_id", "book_id"}),
        indexes = @Index(name = "idx_completion_user_completed_at", columnList = "user_id, completed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookCompletion {

    /** 표지 색을 고르지 않았을 때 (딥그린) */
    public static final String DEFAULT_COVER_COLOR = "#1B4D3E";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_completion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_completion_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_completion_book"))
    private Book book;

    /**
     * 꽂혀 있는 책장. 책장 기능 이전에 만든 책이나 완독 인증으로 새로 생긴 책은 null 일 수 있고,
     * 책장 목록을 불러올 때 첫 번째 책장으로 채워진다 ({@code BookshelfService#ensureDefaultShelf}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", foreignKey = @ForeignKey(name = "fk_completion_bookshelf"))
    private Bookshelf shelf;

    /** 사용자가 책을 다 읽은 날짜 */
    @Column(name = "completed_at", nullable = false)
    private LocalDate completedAt;

    /** 책장에 보이는 표지 색 (Hex) */
    @Column(name = "cover_color", nullable = false, length = 7)
    private String coverColor = DEFAULT_COVER_COLOR;

    /** 완독 인증용 독후감 (200자 이상). 인증 전이면 null */
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    /** 완독 인증 시각. 인증 전이면 null */
    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    /**
     * 책장에서 사용자가 직접 정한 위치 (0부터, 작을수록 앞). 정한 적 없으면 null.
     * null 인 책(새로 꽂은 책 포함)은 맨 앞에 최근 완독 순으로 놓인다.
     */
    @Column(name = "shelf_order")
    private Integer shelfOrder;

    /**
     * 보관함에 넣은 책인지. 보관한 책은 책장에서 빠지지만 기록(독후감·인증·포인트)은 그대로 남는다.
     * 인증한 책은 삭제할 수 없으므로, 책장을 정리하고 싶을 때 여기로 옮긴다.
     */
    @ColumnDefault("false")
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    /** 이 인증으로 지급된 포인트 (보상 정책이 바뀌어도 당시 지급 내역을 남기기 위함) */
    @Column(name = "earned_point", nullable = false)
    private int earnedPoint = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public BookCompletion(User user, Book book, Bookshelf shelf, LocalDate completedAt, String coverColor) {
        this.user = user;
        this.shelf = shelf;
        this.book = book;
        this.completedAt = completedAt;
        this.coverColor = coverColor == null ? DEFAULT_COVER_COLOR : coverColor;
    }

    // === 비즈니스 메서드 ===

    public boolean isCertified() {
        return certifiedAt != null;
    }

    /** 완독 인증 처리 — 독후감과 지급 포인트를 기록 */
    public void certify(String reviewText, LocalDate completedAt, int earnedPoint, LocalDateTime now) {
        if (isCertified()) {
            throw new IllegalStateException("이미 인증된 완독 기록입니다. id=" + id);
        }
        this.reviewText = reviewText;
        this.completedAt = completedAt;
        this.earnedPoint = earnedPoint;
        this.certifiedAt = now;
    }

    /** 인증 전 기록의 책 / 완독 날짜 변경 */
    public void changeBook(Book book, LocalDate completedAt) {
        if (isCertified()) {
            throw new IllegalStateException("인증된 완독 기록은 책 정보를 바꿀 수 없습니다. id=" + id);
        }
        this.book = book;
        this.completedAt = completedAt;
    }

    /** 책장이 없던 책을 채워 넣을 때 — 순서는 그대로 둔다 */
    public void assignShelf(Bookshelf shelf) {
        this.shelf = shelf;
    }

    /** 다른 책장으로 옮기면 그 책장의 맨 앞(최근 완독 순)에 놓인다 */
    public void moveToShelf(Bookshelf shelf) {
        this.shelf = shelf;
        this.shelfOrder = null;
    }

    public void archive() {
        this.archived = true;
        this.shelfOrder = null;
    }

    /** 보관함에서 꺼내면 새로 꽂은 책처럼 맨 앞(최근 완독 순)으로 돌아간다 */
    public void unarchive() {
        this.archived = false;
        this.shelfOrder = null;
    }

    public void moveTo(int shelfOrder) {
        this.shelfOrder = shelfOrder;
    }

    public void changeCoverColor(String coverColor) {
        this.coverColor = coverColor;
    }
}
