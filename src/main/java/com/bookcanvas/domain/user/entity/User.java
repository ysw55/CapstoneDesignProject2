package com.bookcanvas.domain.user.entity;

import com.bookcanvas.domain.quote.entity.Quote;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * - 프로필 스킨 보상을 위한 profileBackgroundColor, profileBorderColor 필드 포함 (Hex Code)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 해시. 소셜(시연용) 계정은 사용하지 않는 무작위 값 */
    @Column(nullable = false)
    private String password;

    /** 가입 방식 — 소셜 계정은 비밀번호 변경 불가 */
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LOCAL'")
    @Column(nullable = false, length = 10)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 200)
    private String bio;

    /**
     * 프로필 배경 색상 (Hex Code, 예: "#FFFFFF")
     * 보상 시스템을 통해 획득 가능
     */
    @Column(name = "profile_background_color", length = 7)
    private String profileBackgroundColor;

    /**
     * 프로필 테두리 색상 (Hex Code, 예: "#000000")
     * 보상 시스템을 통해 획득 가능
     */
    @Column(name = "profile_border_color", length = 7)
    private String profileBorderColor;

    /**
     * 완독 인증 등으로 쌓이는 보상 포인트.
     * {@code @ColumnDefault}: 이미 데이터가 있는 테이블에 컬럼을 추가해도 기존 행이 0 으로 채워짐
     */
    @ColumnDefault("0")
    @Column(name = "reward_point", nullable = false)
    private int rewardPoint = 0;

    // 사용자가 등록한 구절 목록 (양방향 관계, 읽기 전용 참조)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quote> quotes = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String username, String email, String password, String bio,
                String profileBackgroundColor, String profileBorderColor, AuthProvider provider) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.provider = provider == null ? AuthProvider.LOCAL : provider;
        this.bio = bio;
        this.profileBackgroundColor = profileBackgroundColor;
        this.profileBorderColor = profileBorderColor;
    }

    // === 비즈니스 메서드 ===

    public void updateProfile(String bio) {
        this.bio = bio;
    }

    /**
     * 비밀번호 변경 (이미 암호화된 값을 받는다)
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public boolean isSocialAccount() {
        return provider != AuthProvider.LOCAL;
    }

    /**
     * 보상으로 획득한 스킨을 프로필에 적용
     */
    public void applyProfileSkin(String backgroundColorHex, String borderColorHex) {
        this.profileBackgroundColor = backgroundColorHex;
        this.profileBorderColor = borderColorHex;
    }

    /**
     * 보상 포인트 적립
     */
    public void addRewardPoint(int point) {
        if (point < 0) {
            throw new IllegalArgumentException("적립 포인트는 0 이상이어야 합니다.");
        }
        this.rewardPoint += point;
    }
}
