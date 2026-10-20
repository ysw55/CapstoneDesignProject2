import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import styled from 'styled-components';
import { TABS } from './tabs';

export default function BottomTabBar() {
  const { pathname } = useLocation();

  // 이미 선택된 탭을 다시 누르면 맨 위로 스크롤 (네이티브 앱 관례)
  const handleClick = (path) => {
    if (pathname === path) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <Bar aria-label="메인 메뉴">
      <TabList>
        {TABS.map(({ path, label, icon: Icon }) => (
          <li key={path}>
            <TabLink to={path} end={path === '/'} onClick={() => handleClick(path)}>
              {({ isActive }) => (
                <>
                  <IconWrap $active={isActive}>
                    <Icon size={24} strokeWidth={isActive ? 2.2 : 1.7} aria-hidden />
                  </IconWrap>
                  <Label $active={isActive}>{label}</Label>
                </>
              )}
            </TabLink>
          </li>
        ))}
      </TabList>
    </Bar>
  );
}

const Bar = styled.nav`
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: 100%;
  max-width: ${({ theme }) => theme.layout.maxWidth};
  /* iOS 홈 인디케이터 영역만큼 아래 여백 확보 */
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: ${({ theme }) => theme.colors.tabBar};
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const TabList = styled.ul`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  height: ${({ theme }) => theme.layout.tabBarHeight};
`;

const TabLink = styled(NavLink)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 100%;
  user-select: none;
  transition: transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.92);
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: -6px;
    border-radius: ${({ theme }) => theme.radius.md};
  }
`;

const IconWrap = styled.span`
  display: grid;
  place-items: center;
  height: 28px;
  color: ${({ $active, theme }) => ($active ? theme.colors.accent : theme.colors.textTertiary)};
  transition: color ${({ theme }) => theme.motion.normal};
`;

const Label = styled.span`
  font-size: ${({ theme }) => theme.fontSize.caption};
  font-weight: ${({ $active }) => ($active ? 700 : 500)};
  letter-spacing: -0.02em;
  color: ${({ $active, theme }) => ($active ? theme.colors.accent : theme.colors.textTertiary)};
  transition: color ${({ theme }) => theme.motion.normal};
`;
