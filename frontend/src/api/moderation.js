import { apiFetch } from './client';

/** 신고 · 차단 API (모두 JWT 필수) */

/** @param {'SPAM'|'ABUSE'|'ADULT'|'COPYRIGHT'|'OTHER'} reason */
export const reportQuote = (quoteId, reason) =>
  apiFetch(`/api/quotes/${quoteId}/report`, { method: 'POST', body: { reason } });

export const blockUser = (userId) => apiFetch(`/api/users/${userId}/block`, { method: 'POST' });
export const unblockUser = (userId) => apiFetch(`/api/users/${userId}/block`, { method: 'DELETE' });

/** @returns {Promise<{ userId, username, profileBackgroundColor, profileBorderColor, profileImage, profileFrame }[]>} */
export const fetchBlockedUsers = () => apiFetch('/api/users/me/blocks');
