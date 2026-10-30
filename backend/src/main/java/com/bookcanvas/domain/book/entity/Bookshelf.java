package com.bookcanvas.domain.book.entity;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자의 책장 하나. 사용자는 책장을 여러 개(최대 {@link #MAX_PER_USER}개) 만들어
 * 주제별로 책을 나눠 꽂을 수 있다. 책 한 권({@link BookCompletion})은 책장 하나에만 속한다.
 */
@Entity
@Table(name = "bookshelf",
        indexes = @Index(name = "idx_bookshelf_user_position", columnList = "user_id, position"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Bookshelf {

    public static final int MAX_PER_USER = 5;
    public static final int MAX_NAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookshelf_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookshelf_user"))
    private User user;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    /** 책장 탭에 보이는 순서 (0부터) */
    @Column(name = "position", nullable = false)
    private int position;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Bookshelf(User user, String name, int position) {
        this.user = user;
        this.name = name;
        this.position = position;
    }

    public void rename(String name) {
        this.name = name;
    }
}
