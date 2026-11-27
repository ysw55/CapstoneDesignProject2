import { apiFetch } from './client';

/**
 * 회원 인증 API (Spring Boot /api/auth)
 * 성공 응답: { accessToken, tokenType: 'Bearer', expiresIn, user }
 */

/** @param {{ email: string, password: string, username: string }} payload */
export const signup = (payload) => apiFetch('/api/auth/signup', { method: 'POST', body: payload, auth: false });

/** @param {{ email: string, password: string }} payload */
export const login = (payload) => apiFetch('/api/auth/login', { method: 'POST', body: payload, auth: false });

/** 저장된 토큰으로 내 정보 조회 (앱 재실행 시 로그인 상태 복원) */
export const fetchMe = () => apiFetch('/api/auth/me');

/** @param {{ currentPassword: string, newPassword: string }} payload — 성공 시 204 */
export const changePassword = (payload) => apiFetch('/api/auth/password', { method: 'POST', body: payload });

/** 회원탈퇴 — 소셜 계정은 password 없이. 성공 시 204 */
export const withdraw = (password) => apiFetch('/api/auth/withdraw', { method: 'POST', body: { password } });

/**
 * 시연용 간편 로그인 — 실제 카카오/구글 OAuth 가 아니라 provider 별 시연 계정으로 로그인
 * @param {'kakao' | 'google'} provider
 */
export const demoLogin = (provider) =>
  apiFetch('/api/auth/demo-login', { method: 'POST', body: { provider }, auth: false });

/** 프로필 꾸미기 — 배경색/테두리색(#RRGGBB)와 프로필 사진(data URL), 각각 null 이면 해제. 갱신된 내 정보를 돌려준다 */
export const updateProfileSkin = (payload) => apiFetch('/api/auth/profile-skin', { method: 'PUT', body: payload });
