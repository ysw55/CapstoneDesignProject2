/** 1204 → "1,204" */
export const formatCount = (n) => n.toLocaleString('ko-KR');

/** "2026-10-12" → "2026.10.12" */
export const formatDate = (iso) => iso.replaceAll('-', '.');

/** 기기 로컬 시간 기준 오늘 날짜 "2026-10-19" (toISOString 은 UTC 라 한국에서 하루가 밀릴 수 있음) */
export function todayISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/**
 * 서버 시각("2026-10-20T11:05:00", 한국 시간 기준) → "방금 전 / 12분 전 / 3시간 전 / 어제 / 10월 3일"
 */
export function relativeTime(isoDateTime) {
  if (!isoDateTime) return '';
  const date = new Date(isoDateTime); // 시간대 표기가 없는 ISO 문자열은 기기 로컬 시간으로 해석됨
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffMin < 60 * 24) return `${Math.floor(diffMin / 60)}시간 전`;
  if (diffMin < 60 * 48) return '어제';

  const sameYear = date.getFullYear() === new Date().getFullYear();
  return sameYear
    ? `${date.getMonth() + 1}월 ${date.getDate()}일`
    : `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
}

/** 배경색이 밝으면 true — 그 위에 올릴 글자색을 고를 때 사용 */
export function isLightColor(hex) {
  const v = hex.replace('#', '');
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(v.slice(i, i + 2), 16));
  return 0.299 * r + 0.587 * g + 0.114 * b > 160;
}
