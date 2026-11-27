/**
 * 상점 상품 목록 = 프로필 테두리 장식 (서버 domain/shop/ShopItem 과 코드·가격을 맞춘다. 실제 가격 검증은 서버에서 한다)
 * 코드 'BORDER_RRGGBB' 는 해당 색의 단색 링, 그 밖의 코드는 ProfileAvatar 의 FRAMES 에 정의된 장식.
 * color 는 선택 칩에 쓰는 대표 색.
 */
const RING_PRICE = 50;

/** 배경색은 무료 — 프로필 꾸미기에서 바로 고른다 */
export const BACKGROUNDS = ['#2F6B4F', '#3B82F6', '#8B5CF6', '#EC4899', '#EF4444', '#F59E0B', '#FDE68A', '#212529'];
const RING_COLORS = ['#FFFFFF', '#FBBF24', '#34D399', '#60A5FA', '#A78BFA', '#F472B6', '#F87171', '#212529'];

export const FRAME_ITEMS = [
  ...RING_COLORS.map((color, i) => ({
    code: `BORDER_${color.slice(1).toUpperCase()}`,
    color,
    name: `단색 링 ${i + 1}`,
    description: '',
    price: RING_PRICE,
  })),
  {
    code: 'DEMIAN',
    color: 'linear-gradient(135deg, #E8C766 50%, #2B2D5B 50%)',
    name: '데미안 · 두 세계',
    description: '『데미안』에서 영감을 받은, 밝음과 어둠이 맞닿은 테두리',
    price: 0,
  },
  { code: 'GOLD_RING', color: '#D4A017', name: '골드 링', description: '심플한 금색 이중 테두리', price: 100 },
];

export const SHOP_SECTIONS = [{ title: '테두리 장식', items: FRAME_ITEMS }];
