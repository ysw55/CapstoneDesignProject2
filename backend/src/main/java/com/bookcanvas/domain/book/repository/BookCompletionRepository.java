package com.bookcanvas.domain.book.repository;

import com.bookcanvas.domain.book.entity.BookCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 내 책장(완독 기록) 레포지토리
 */
public interface BookCompletionRepository extends JpaRepository<BookCompletion, Long> {

    Optional<BookCompletion> findByUserIdAndBookId(Long userId, Long bookId);

    /** 다른 사용자의 책장 항목에 접근하지 못하도록 항상 userId 와 함께 조회 */
    Optional<BookCompletion> findByIdAndUserId(Long id, Long userId);

    /**
     * 내 책장 전체. 책 정보는 fetch join 으로 한 번에.
     * 순서: 직접 정한 위치가 없는 책(최근 완독 순) → 직접 정한 위치 순
     */
    @Query("""
            SELECT c FROM BookCompletion c
            JOIN FETCH c.book
            WHERE c.user.id = :userId
            ORDER BY CASE WHEN c.shelfOrder IS NULL THEN 0 ELSE 1 END,
                     c.shelfOrder ASC, c.completedAt DESC, c.id DESC
            """)
    List<BookCompletion> findShelfByUserId(@Param("userId") Long userId);

    /** 책장이 정해지지 않은 책 (책장 기능 이전 데이터, 완독 인증으로 새로 생긴 책) */
    List<BookCompletion> findByUserIdAndShelfIsNull(Long userId);

    List<BookCompletion> findByShelfId(Long shelfId);

    /** 책장별 꽂힌 책 수 (보관함 제외) — [shelfId, count] */
    @Query("""
            SELECT c.shelf.id, COUNT(c) FROM BookCompletion c
            WHERE c.user.id = :userId AND c.archived = false AND c.shelf IS NOT NULL
            GROUP BY c.shelf.id
            """)
    List<Object[]> countByShelf(@Param("userId") Long userId);

    /** 완독 '인증'한 책 수 (책장에 추가만 한 책은 제외) */
    long countByUserIdAndCertifiedAtIsNotNull(Long userId);
}
