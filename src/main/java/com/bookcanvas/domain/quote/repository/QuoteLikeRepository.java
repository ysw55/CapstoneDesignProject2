package com.bookcanvas.domain.quote.repository;

import com.bookcanvas.domain.quote.entity.QuoteLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuoteLikeRepository extends JpaRepository<QuoteLike, Long> {

    boolean existsByUserIdAndQuoteId(Long userId, Long quoteId);

    long countByQuoteId(Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteLike l WHERE l.user.id = :userId AND l.quote.id = :quoteId")
    int deleteByUserIdAndQuoteId(@Param("userId") Long userId, @Param("quoteId") Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteLike l WHERE l.quote.id = :quoteId")
    void deleteAllByQuoteId(@Param("quoteId") Long quoteId);

    /** 피드 한 페이지의 좋아요 수를 한 번의 GROUP BY 쿼리로 (N+1 방지). 결과: [quoteId, count] */
    @Query("SELECT l.quote.id, COUNT(l) FROM QuoteLike l WHERE l.quote.id IN :quoteIds GROUP BY l.quote.id")
    List<Object[]> countGroupByQuoteIds(@Param("quoteIds") Collection<Long> quoteIds);

    /** 피드 한 페이지 중 내가 좋아요한 글 ID */
    @Query("SELECT l.quote.id FROM QuoteLike l WHERE l.user.id = :userId AND l.quote.id IN :quoteIds")
    List<Long> findLikedQuoteIds(@Param("userId") Long userId, @Param("quoteIds") Collection<Long> quoteIds);
}
