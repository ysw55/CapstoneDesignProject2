package com.bookcanvas.domain.quote.repository;

import com.bookcanvas.domain.quote.entity.QuoteBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuoteBookmarkRepository extends JpaRepository<QuoteBookmark, Long> {

    boolean existsByUserIdAndQuoteId(Long userId, Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteBookmark b WHERE b.user.id = :userId AND b.quote.id = :quoteId")
    int deleteByUserIdAndQuoteId(@Param("userId") Long userId, @Param("quoteId") Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteBookmark b WHERE b.quote.id = :quoteId")
    void deleteAllByQuoteId(@Param("quoteId") Long quoteId);

    /** 피드 한 페이지 중 내가 저장한 글 ID */
    @Query("SELECT b.quote.id FROM QuoteBookmark b WHERE b.user.id = :userId AND b.quote.id IN :quoteIds")
    List<Long> findBookmarkedQuoteIds(@Param("userId") Long userId, @Param("quoteIds") Collection<Long> quoteIds);

    /** 내가 저장한 글 — 최근 저장한 순. 구절·작성자·도서는 fetch join 으로 한 번에 */
    @Query(value = """
            SELECT b FROM QuoteBookmark b
            JOIN FETCH b.quote q JOIN FETCH q.user JOIN FETCH q.book
            WHERE b.user.id = :userId AND q.isPublic = true
            ORDER BY b.createdAt DESC, b.id DESC
            """,
            countQuery = "SELECT COUNT(b) FROM QuoteBookmark b WHERE b.user.id = :userId AND b.quote.isPublic = true")
    Page<QuoteBookmark> findMyBookmarks(@Param("userId") Long userId, Pageable pageable);
}
