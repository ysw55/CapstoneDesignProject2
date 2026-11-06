import { apiFetch } from './client';

/**
 * 커뮤니티 피드 API (Spring Boot /api/quotes)
 * 피드·댓글 조회는 비로그인도 가능하고, 토큰이 있으면 liked / mine 이 계산되어 온다.
 */

/** @returns {Promise<{ content: object[], pageNumber, pageSize, totalElements, totalPages, first, last }>} */
export const fetchFeed = ({ page = 0, size = 30 } = {}) => apiFetch(`/api/quotes?page=${page}&size=${size}`);

/** @param {{ bookTitle, author, content, tagName }} payload */
export const createQuote = (payload) => apiFetch('/api/quotes', { method: 'POST', body: payload });

export const updateQuote = (quoteId, payload) => apiFetch(`/api/quotes/${quoteId}`, { method: 'PUT', body: payload });

export const deleteQuote = (quoteId) => apiFetch(`/api/quotes/${quoteId}`, { method: 'DELETE' });

/** @returns {Promise<{ quoteId, liked, likeCount }>} */
export const likeQuote = (quoteId) => apiFetch(`/api/quotes/${quoteId}/like`, { method: 'POST' });
export const unlikeQuote = (quoteId) => apiFetch(`/api/quotes/${quoteId}/like`, { method: 'DELETE' });

/** 북마크(저장) — 같은 요청을 여러 번 보내도 결과가 같다 @returns {Promise<{ quoteId, bookmarked }>} */
export const bookmarkQuote = (quoteId) => apiFetch(`/api/quotes/${quoteId}/bookmark`, { method: 'POST' });
export const unbookmarkQuote = (quoteId) => apiFetch(`/api/quotes/${quoteId}/bookmark`, { method: 'DELETE' });

/**
 * 내 활동 (JWT 필수) — 프로필의 "좋아요 · 저장한 문장". 최근에 누른 순
 * @returns {Promise<{ content: { quote: object, actedAt: string }[], totalElements, last, ... }>}
 */
export const fetchMyLiked = ({ page = 0, size = 20 } = {}) => apiFetch(`/api/quotes/me/liked?page=${page}&size=${size}`);
export const fetchMyBookmarked = ({ page = 0, size = 20 } = {}) =>
  apiFetch(`/api/quotes/me/bookmarked?page=${page}&size=${size}`);

export const fetchComments = (quoteId) => apiFetch(`/api/quotes/${quoteId}/comments`);

export const createComment = (quoteId, content) =>
  apiFetch(`/api/quotes/${quoteId}/comments`, { method: 'POST', body: { content } });
