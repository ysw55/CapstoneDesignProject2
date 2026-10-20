/**
 * 프론트에 고정으로 두는 데이터.
 * - 커뮤니티 피드 / 내 책장 / 사용자는 백엔드 DB 에서 불러온다 (서버 시작 시 시연용 샘플이 자동 입력됨)
 * - 랭킹은 아직 집계 API 가 없어 샘플 데이터를 사용한다
 * 인용문은 국내 번역본마다 표현이 조금씩 다를 수 있다.
 */

/* ───────── 상황 태그 ─────────
 * 서버 tag.name 과 label 이 같아야 한다 (백엔드 DefaultTagInitializer 와 동일한 목록) */
export const TAG_OPTIONS = [
  { emoji: '💧', label: '위로가 필요할 때' },
  { emoji: '🌱', label: '새로운 시작 앞에서' },
  { emoji: '🌙', label: '마음이 조급할 때' },
  { emoji: '🔥', label: '다시 일어서고 싶을 때' },
  { emoji: '✨', label: '꿈을 응원받고 싶을 때' },
  { emoji: '☕', label: '여유가 필요할 때' },
  { emoji: '💌', label: '사랑이 어려울 때' },
];

/** 서버 태그 이름 → { emoji, label } (목록에 없으면 기본 이모지) */
export const tagFromName = (name) => TAG_OPTIONS.find((t) => t.label === name) ?? { emoji: '🏷️', label: name };

/* 새로 꽂히는 책에 순서대로 돌아가며 입히는 표지 색 (딥그린 + 무채색) */
export const COVER_PALETTE = ['#1B4D3E', '#212529', '#E9ECEF', '#2F6B57', '#495057', '#ADB5BD'];

/* ───────── 랭킹 ───────── */
export const monthlyBestBooks = [
  { rank: 1, title: '데미안', author: '헤르만 헤세', recommendCount: 2481, coverColor: '#1B4D3E' },
  { rank: 2, title: '어린 왕자', author: '앙투안 드 생텍쥐페리', recommendCount: 2105, coverColor: '#212529' },
  { rank: 3, title: '위대한 개츠비', author: 'F. 스콧 피츠제럴드', recommendCount: 1872, coverColor: '#ADB5BD' },
];

export const weeklyBestQuotes = [
  {
    rank: 1,
    quote: '인간은 파멸당할 수는 있어도 패배하지는 않아.',
    bookTitle: '노인과 바다',
    author: '어니스트 헤밍웨이',
    userName: '종이비행기',
    likeCount: 1204,
  },
  {
    rank: 2,
    quote: '왜 살아야 하는지 아는 사람은 그 어떤 상황도 견딜 수 있다.',
    bookTitle: '죽음의 수용소에서',
    author: '빅터 프랭클',
    userName: '오늘도한장',
    likeCount: 987,
  },
  {
    rank: 3,
    quote: '그래서 우리는 물결을 거스르는 배처럼, 쉴 새 없이 과거 속으로 밀려나면서도 앞으로 나아가는 것이다.',
    bookTitle: '위대한 개츠비',
    author: 'F. 스콧 피츠제럴드',
    userName: '새벽책방',
    likeCount: 845,
  },
];
