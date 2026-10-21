package com.bookcanvas.domain.book.entity;

import com.bookcanvas.domain.quote.entity.Quote;
import jakarta.persistence.*;
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

    @Column(columnDefinition = "TEXT")
    private String description;

    // 이 도서에 등록된 구절 목록 (양방향 관계, 읽기 전용 참조)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quote> quotes = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
