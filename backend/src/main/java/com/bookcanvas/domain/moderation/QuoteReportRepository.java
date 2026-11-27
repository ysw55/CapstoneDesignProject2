package com.bookcanvas.domain.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteReportRepository extends JpaRepository<QuoteReport, Long> {

    boolean existsByReporterIdAndQuoteId(Long reporterId, Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteReport r WHERE r.quote.id = :quoteId")
    void deleteAllByQuoteId(@Param("quoteId") Long quoteId);
}
