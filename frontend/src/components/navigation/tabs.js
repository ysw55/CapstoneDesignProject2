import { LibraryBig, MessageSquareQuote, Trophy, UserRound } from 'lucide-react';

/** 하단 탭 정의 — 탭 추가/순서 변경은 여기서만 하면 된다. */
export const TABS = [
  { path: '/', label: '커뮤니티', icon: MessageSquareQuote },
  { path: '/ranking', label: '랭킹', icon: Trophy },
  { path: '/bookshelf', label: '책장', icon: LibraryBig },
  { path: '/profile', label: '프로필', icon: UserRound },
];
