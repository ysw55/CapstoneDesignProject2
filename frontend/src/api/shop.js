import { apiFetch } from './client';

/** 상점 상품 구매 — 포인트 차감 후 갱신된 내 정보를 돌려준다 (이미 보유/포인트 부족이면 오류) */
export const purchaseItem = (code) => apiFetch(`/api/shop/items/${code}/purchase`, { method: 'POST' });
