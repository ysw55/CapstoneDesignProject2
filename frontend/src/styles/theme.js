/**
 * Book Canvas 디자인 토큰
 * - 화이트 / 블랙 / 딥그린 3색을 기준으로 한다.
 * - 컴포넌트에서는 색상 값을 직접 쓰지 말고 항상 theme.colors.* 를 사용한다.
 * - light / dark 는 colors·shadow 만 다르고 나머지 토큰(간격, 폰트, 레이아웃)은 공유한다.
 */

const base = {
  font: {
    sans: `'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif`,
    serif: `'Noto Serif KR', 'Nanum Myeongjo', serif`, // 인용 문장 전용
  },
  fontSize: {
    caption: '11px',
    sm: '13px',
    md: '15px',
    lg: '17px',
    xl: '22px',
    title: '28px',
  },
  space: (n) => `${n * 4}px`, // 4px 그리드: space(4) === '16px'
  radius: {
    sm: '8px',
    md: '12px',
    lg: '16px',
    pill: '999px',
  },
  layout: {
    maxWidth: '430px',
    gutter: '20px',
    tabBarHeight: '64px',
  },
  motion: {
    fast: '150ms cubic-bezier(0.2, 0, 0, 1)',
    normal: '250ms cubic-bezier(0.2, 0, 0, 1)',
  },
  // 내 책장 가구: 호두나무 프레임 · 밝은 오크 선반 · 어두운 원목 뒷판. 라이트/다크 모두 같은 색을 쓴다.
  furniture: {
    frame: '#8A5A3B', // 기둥·받침 (호두나무)
    line: '#3B2315', // 부위를 나누는 윤곽선
    groove: '#65402A', // 기둥에 판 세로 홈
    bevel: '#C08F63', // 기둥 안쪽 모서리 하이라이트
    carving: 'rgba(244, 222, 190, 0.7)', // 조각 무늬 (밝은 선)
    carvingShadow: 'rgba(45, 24, 12, 0.6)', // 조각 무늬 (파인 그림자)
    top: '#A8794F', // 맨 위 상판
    plank: '#B88B5E', // 선반 (오크)
    plankEdge: '#D4AB7C', // 선반 윗면 반사광
    wood: '#6A4430', // 뒷판
    woodDark: '#4B2F21',
    trim: '#C9A24E',
    label: '#F1E6D6',
    labelMuted: '#C9B8A0',
  },
};

export const lightTheme = {
  ...base,
  mode: 'light',
  colors: {
    backdrop: '#F8F9FA', // 데스크톱에서 앱 프레임 바깥
    background: '#FFFFFF', // 메인 화면 배경
    backgroundAlt: '#F8F9FA', // 설정처럼 카드를 묶어 보여주는 화면의 배경
    surface: '#FFFFFF', // 카드, 리스트 그룹
    surfaceMuted: '#F1F3F5', // 눌림 상태, 이미지 자리
    border: '#EDEFF2',
    textPrimary: '#111111',
    textSecondary: '#495057',
    textTertiary: '#868E96',
    accent: '#1B4D3E', // 딥그린: 활성 탭, 스위치 ON, 주요 버튼
    accentPressed: '#133A2F',
    accentSoft: 'rgba(27, 77, 62, 0.08)',
    onAccent: '#FFFFFF',
    tabBar: 'rgba(255, 255, 255, 0.92)',
    switchOff: '#DEE2E6',
    danger: '#E03131',
    dangerSoft: 'rgba(224, 49, 49, 0.08)',
  },
  shadow: {
    frame: '0 0 0 1px #EDEFF2',
    card: '0 1px 2px rgba(17, 17, 17, 0.04)',
  },
};

/** 설정의 다크 모드 토글용. 딥그린은 검은 배경에서 안 보이므로 한 톤 밝게 쓴다. */
export const darkTheme = {
  ...base,
  mode: 'dark',
  colors: {
    backdrop: '#000000',
    background: '#111111',
    backgroundAlt: '#0A0A0A',
    surface: '#1A1A1A',
    surfaceMuted: '#242424',
    border: '#262626',
    textPrimary: '#F8F9FA',
    textSecondary: '#ADB5BD',
    textTertiary: '#7A828A',
    accent: '#6CC4A1',
    accentPressed: '#58B08E',
    accentSoft: 'rgba(108, 196, 161, 0.12)',
    onAccent: '#0B2A20',
    tabBar: 'rgba(17, 17, 17, 0.92)',
    switchOff: '#3A3A3A',
    danger: '#FF6B6B',
    dangerSoft: 'rgba(255, 107, 107, 0.12)',
  },
  shadow: {
    frame: '0 0 0 1px #1F1F1F',
    card: 'none',
  },
};
