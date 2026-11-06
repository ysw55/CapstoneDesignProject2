import { apiFetch } from './client';

/** 서버와 같은 기준: 앞뒤 공백을 뺀 String.length (UTF-16) */
export const MIN_REVIEW_LENGTH = 200;
export const MAX_REVIEW_LENGTH = 5000;

/**
 * 내 책장 (JWT 필수)
 * 응답 항목: { completionId, book: { bookId, title, author }, completedAt, coverColor, certified, certifiedAt, earnedPoint, shelfOrder, archived, shelfId, paletteColors, spineImageUrl, productUrl, yes24Pending }
 */
export const fetchShelf = () => apiFetch('/api/books/completions');

/**
 * 도서 검색 (예스24 Open API, 10권씩) — 검색을 쓸 수 없으면 503 BOOK_SEARCH_UNAVAILABLE
 * @returns {Promise<{ results: { title, author, publisher, isbn, coverImageUrl, spineImageUrl, productUrl, publishedAt }[], hasMore: boolean }>}
 */
export const searchBooks = (query, page = 1) =>
  apiFetch(`/api/books/search?q=${encodeURIComponent(query)}&page=${page}`);

/**
 * 기존 책을 예스24 책 정보(표지·책등·상품 링크)로 한꺼번에 연결 (한 번에 30권까지)
 * @returns {Promise<{ linked: number, notFound: number, remaining: number, stopped: boolean }>}
 */
export const linkYes24Books = () => apiFetch('/api/books/completions/link-yes24', { method: 'POST' });

/** 내 책장(여러 개) 목록 — 응답 항목: { shelfId, name, position, bookCount } */
export const fetchShelves = () => apiFetch('/api/books/shelves');

/** @param {string} [name] 비우면 "책장 N" */
export const createShelf = (name) => apiFetch('/api/books/shelves', { method: 'POST', body: { name } });

export const renameShelf = (shelfId, name) => apiFetch(`/api/books/shelves/${shelfId}`, { method: 'PUT', body: { name } });

/** 책장 삭제 — 꽂혀 있던 책은 첫 번째 책장으로 옮겨진다 */
export const deleteShelf = (shelfId) => apiFetch(`/api/books/shelves/${shelfId}`, { method: 'DELETE' });

/** 책을 다른 책장으로 옮기기 (그 책장의 맨 앞에 놓인다) */
export const moveShelfBook = (completionId, shelfId) =>
  apiFetch(`/api/books/completions/${completionId}/shelf`, { method: 'PUT', body: { shelfId } });

/**
 * 독후감 없이 책장에만 추가
 * @param {{ title, author, completedAt, coverColor, shelfId?, isbn?, publisher?, coverImageUrl?, spineImageUrl?, productUrl? }} payload
 *   shelfId 를 비우면 첫 번째 책장, isbn 이하는 도서 검색으로 고른 책일 때
 */
export const addShelfBook = (payload) => apiFetch('/api/books/completions', { method: 'POST', body: payload });

export const updateShelfBook = (completionId, payload) =>
  apiFetch(`/api/books/completions/${completionId}`, { method: 'PUT', body: payload });

/** 책장 순서 저장 — 그 책장에 꽂힌 모든 completionId 를 원하는 순서(앞 → 뒤)로 */
export const reorderShelf = (completionIds, shelfId) =>
  apiFetch('/api/books/completions/order', { method: 'PUT', body: { shelfId, completionIds } });

/** 보관함으로 옮기기 (인증한 책도 가능, 기록·포인트는 그대로) */
export const archiveShelfBook = (completionId) =>
  apiFetch(`/api/books/completions/${completionId}/archive`, { method: 'POST' });

/** 보관함에서 책장으로 꺼내기 (맨 앞에 꽂힌다) — shelfId 를 비우면 보관하기 전 책장 */
export const unarchiveShelfBook = (completionId, shelfId) =>
  apiFetch(`/api/books/completions/${completionId}/archive${shelfId ? `?shelfId=${shelfId}` : ''}`, { method: 'DELETE' });

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
