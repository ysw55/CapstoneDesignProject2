package com.bookcanvas.domain.quote.entity;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.tag.entity.Tag;
import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 구절(Quote) 엔티티
 * - 사용자(User)가 특정 도서(Book)에서 발췌한 문장
 * - Tag와 N:M 관계 → QuoteTagMap 중간 테이블로 매핑
 */
@Entity
@Table(name = "quote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long id;

    /** 발췌한 구절 원문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 사용자의 감상/메모 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 도서 내 페이지 번호 (선택) */
    @Column(name = "page_number")
    private Integer pageNumber;

    /** 공개 여부 (true: 공개, false: 비공개) */
    @Column(nullable = false)
    private boolean isPublic = true;

    // === 연관 관계 ===

    /**
     * 구절을 등록한 사용자 (N:1)
     * Quote 입장에서 Many, User 입장에서 One
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_user"))
    private User user;

    /**
     * 구절이 속한 도서 (N:1)
     * Quote 입장에서 Many, Book 입장에서 One
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_book"))
    private Book book;

    /**
     * 태그 매핑 목록 (QuoteTagMap 중간 테이블을 통한 N:M)
     *
     * <p>{@code @BatchSize}: 태그 기반 목록 조회 시 N+1 방지.
     * Quote N건 조회 후, quoteTagMaps 컬렉션을 100건씩 IN 쿼리로 일괄 로딩합니다.
     */
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuoteTagMap> quoteTagMaps = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 사용자가 직접 수정한 시각. 태그만 바꾸는 경우 updatedAt 이 갱신되지 않을 수 있어 별도로 기록
     */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Builder
    public Quote(String content, String memo, Integer pageNumber, boolean isPublic,
                 User user, Book book) {
        this.content = content;
        this.memo = memo;
        this.pageNumber = pageNumber;
        this.isPublic = isPublic;
        this.user = user;
        this.book = book;
    }

    // === 비즈니스 메서드 ===

    public void updateContent(String content, String memo, Integer pageNumber) {
        this.content = content;
        this.memo = memo;
        this.pageNumber = pageNumber;
    }

    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }

    /**
     * 태그 매핑 추가 (편의 메서드)
     */
    public void addQuoteTagMap(QuoteTagMap quoteTagMap) {
        this.quoteTagMaps.add(quoteTagMap);
    }

    /** 다른 책으로 변경 (글 수정) */
    public void changeBook(Book book) {
        this.book = book;
    }

    /**
     * 태그를 주어진 하나로 교체 (현재 UI 는 상황 태그 1개)
     * orphanRemoval 덕분에 빠진 매핑 행은 자동 삭제됩니다.
     */
    public void replaceTag(Tag tag) {
        boolean alreadyTagged = quoteTagMaps.size() == 1 && quoteTagMaps.get(0).getTag().getId().equals(tag.getId());
        if (alreadyTagged) return;
        quoteTagMaps.clear();
        QuoteTagMap.of(this, tag);
    }

    /** 사용자가 글을 수정한 시각 기록 ('수정됨' 표시용) */
    public void markEdited(LocalDateTime now) {
        this.editedAt = now;
    }
}
