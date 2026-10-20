package com.bookcanvas.domain.quote.repository;

import com.bookcanvas.domain.quote.entity.QuoteComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuoteCommentRepository extends JpaRepository<QuoteComment, Long> {

    /** 댓글 목록 — 작성자 정보까지 한 번에 */
    @Query("""
            SELECT c FROM QuoteComment c
            JOIN FETCH c.user
            WHERE c.quote.id = :quoteId
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<QuoteComment> findByQuoteIdWithUser(@Param("quoteId") Long quoteId);

    long countByQuoteId(Long quoteId);

    /** 결과: [quoteId, count] */
    @Query("SELECT c.quote.id, COUNT(c) FROM QuoteComment c WHERE c.quote.id IN :quoteIds GROUP BY c.quote.id")
    List<Object[]> countGroupByQuoteIds(@Param("quoteIds") Collection<Long> quoteIds);

    @Modifying
    @Query("DELETE FROM QuoteComment c WHERE c.quote.id = :quoteId")
    void deleteAllByQuoteId(@Param("quoteId") Long quoteId);
}
