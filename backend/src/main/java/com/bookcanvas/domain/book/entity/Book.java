package com.bookcanvas.domain.book.entity;

import com.bookcanvas.domain.quote.entity.Quote;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 도서 엔티티
 * - 외부 도서 API(카카오, Naver 등) 연동을 고려하여 isbn 필드 포함
 */
@Entity
@Table(name = "book",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_isbn", columnNames = "isbn"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(length = 200)
    private String publisher;

    /** ISBN-13 기준 */
    @Column(length = 20)
    private String isbn;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** 책등 이미지 URL (예스24). 등록되지 않은 책은 null */
    @Column(name = "spine_image_url", length = 500)
    private String spineImageUrl;

    /** 예스24 상품 페이지 — 책 정보를 보여줄 때 연결해야 하는 이용 조건 */
    @Column(name = "product_url", length = 500)
    private String productUrl;

    /** 예스24 에서 같은 책을 찾아 보았는지 (찾지 못한 책을 연결할 때마다 다시 찾지 않도록) */
    @ColumnDefault("false")
    @Column(name = "yes24_checked", nullable = false)
    private boolean yes24Checked = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 표지 전체에서 뽑은 대표 색 3가지 (책등을 3단으로 꾸미는 데 쓴다). "#RRGGBB,#RRGGBB,#RRGGBB" 형식, 많이 나온 순.
     * null = 아직 뽑지 않음, 빈 문자열 = 뽑아 봤지만 쓸 수 없음(다시 시도하지 않는다)
     */
    @Column(name = "cover_palette", length = 40)
    private String coverPalette;

    // 이 도서에 등록된 구절 목록 (양방향 관계, 읽기 전용 참조)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quote> quotes = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 예스24 와 아직 연결되지 않았고 찾아 본 적도 없는 책 */
    public boolean isYes24Pending() {
        return productUrl == null && !yes24Checked;
    }

    /**
     * 기존 책을 예스24 책 정보로 연결한다. 표지는 예스24 것으로 맞추고, ISBN 이 없던 책은 ISBN 도 채운다
     * (다른 책이 이미 쓰는 ISBN 은 호출하는 쪽에서 걸러 넘기지 않는다).
     */
    public void linkYes24(String isbn, String coverImageUrl, String spineImageUrl, String productUrl) {
        this.yes24Checked = true;
        this.productUrl = productUrl;
        if (spineImageUrl != null) this.spineImageUrl = spineImageUrl;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (this.isbn == null && isbn != null) this.isbn = isbn;
    }

    /** 예스24 에서 같은 책을 찾지 못했다고 기록한다 */
    public void markYes24NotFound() {
        this.yes24Checked = true;
    }

    /** 예스24 에서 받은 책등 이미지·상품 링크를 채운다 (이미 있으면 그대로 둔다) */
    public void attachYes24Info(String spineImageUrl, String productUrl) {
        if (this.spineImageUrl == null && spineImageUrl != null) this.spineImageUrl = spineImageUrl;
        if (this.productUrl == null && productUrl != null) this.productUrl = productUrl;
    }

    /** 표지 대표 색 목록 (없으면 빈 목록) */
    public List<String> getCoverPaletteColors() {
        return coverPalette == null || coverPalette.isBlank() ? List.of() : List.of(coverPalette.split(","));
    }

    public boolean hasCoverPaletteResolved() {
        return coverPalette != null;
    }

    public void changeCoverPalette(List<String> colors) {
        this.coverPalette = String.join(",", colors);
    }

    @Builder
    public Book(String title, String author, String publisher,
                String isbn, String coverImageUrl, String description) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.coverImageUrl = coverImageUrl;
        this.description = description;
    }
}
