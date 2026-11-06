package com.bookcanvas.domain.quote.dto;

import java.time.LocalDateTime;

/**
 * 프로필 "좋아요 · 저장한 문장" 목록의 한 항목
 *
 * @param quote   문장 (피드와 같은 형식 — liked / bookmarked 는 지금 상태)
 * @param actedAt 내가 좋아요(또는 저장)를 누른 시각
 */
public record QuoteHistoryResponse(QuoteResponse quote, LocalDateTime actedAt) {
}
