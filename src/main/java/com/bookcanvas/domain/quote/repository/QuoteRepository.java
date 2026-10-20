package com.bookcanvas.domain.quote.repository;

import com.bookcanvas.domain.quote.entity.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Quote 레포지토리
 *
 * <p>작성자(User)·도서(Book)는 N:1 이라 fetch join 해도 페이징(LIMIT)이 정상 동작합니다.
 * 태그 컬렉션은 fetch join 하지 않고 Quote 의 {@code @BatchSize} 로 IN 쿼리 일괄 로딩합니다.
 * (컬렉션 fetch join + 페이징은 HHH90003004 경고와 함께 전체를 메모리로 읽어 옴)
 */
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    /** 전체 공개 피드 (최신순은 Pageable 로 전달) */
    @Query(value = """
            SELECT q FROM Quote q
            JOIN FETCH q.user
            JOIN FETCH q.book
            WHERE q.isPublic = true
            """,
            countQuery = "SELECT COUNT(q) FROM Quote q WHERE q.isPublic = true")
    Page<Quote> findPublicFeed(Pageable pageable);

    /**
     * 주어진 태그 ID 중 하나 이상 포함된 공개 Quote (OR 조건)
     *
     * <p>한 Quote 에 여러 태그가 붙으면 JOIN 결과가 중복될 수 있으므로 서브쿼리로 필터링합니다.
     */
    @Query(value = """
            SELECT q FROM Quote q
            JOIN FETCH q.user
            JOIN FETCH q.book
            WHERE q.isPublic = true
              AND q.id IN (SELECT qtm.quote.id FROM QuoteTagMap qtm WHERE qtm.tag.id IN :tagIds)
            """,
            countQuery = """
            SELECT COUNT(q) FROM Quote q
            WHERE q.isPublic = true
              AND q.id IN (SELECT qtm.quote.id FROM QuoteTagMap qtm WHERE qtm.tag.id IN :tagIds)
            """)
    Page<Quote> findByTagIds(@Param("tagIds") List<Long> tagIds, Pageable pageable);
}
