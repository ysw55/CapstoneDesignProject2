import { apiFetch } from './client';

/** 서버와 같은 기준: 앞뒤 공백을 뺀 String.length (UTF-16) */
export const MIN_REVIEW_LENGTH = 200;
export const MAX_REVIEW_LENGTH = 5000;

/**
 * 내 책장 (JWT 필수)
 * 응답 항목: { completionId, book: { bookId, title, author }, completedAt, coverColor, certified, certifiedAt, earnedPoint, shelfOrder }
 */
export const fetchShelf = () => apiFetch('/api/books/completions');

/** @param {{ title, author, completedAt, coverColor }} payload 독후감 없이 책장에만 추가 */
export const addShelfBook = (payload) => apiFetch('/api/books/completions', { method: 'POST', body: payload });

export const updateShelfBook = (completionId, payload) =>
  apiFetch(`/api/books/completions/${completionId}`, { method: 'PUT', body: payload });

/** 책장 순서 저장 — 내 책장의 모든 completionId 를 원하는 순서(앞 → 뒤)로 */
export const reorderShelf = (completionIds) =>
  apiFetch('/api/books/completions/order', { method: 'PUT', body: { completionIds } });

export const deleteShelfBook = (completionId) =>
  apiFetch(`/api/books/completions/${completionId}`, { method: 'DELETE' });

/**
 * 완독 인증 (JWT 필수) — 책장에 있던 책이면 그 항목이 ✓ 인증 상태가 된다
 * @param {{ title, author, completedAt?, reviewText, coverColor? }} payload
 * @returns {Promise<{
 *   completionId: number,
 *   book: { bookId: number, title: string, author: string, coverImageUrl: string | null },
 *   completedAt: string,
 *   reviewLength: number,
 *   reward: { profileBackgroundColor: string, profileBorderColor: string, earnedPoint: number, totalPoint: number }
 * }>}
 */
export const certifyCompletion = (payload) => apiFetch('/api/books/completion', { method: 'POST', body: payload });
