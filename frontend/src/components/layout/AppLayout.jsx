import React from 'react';
import { Outlet } from 'react-router-dom';
import styled from 'styled-components';
import AppFrame from './AppFrame';
import BottomTabBar from '../navigation/BottomTabBar';
import useScrollTopOnNavigate from './useScrollTopOnNavigate';

/** 하단 탭 바가 있는 메인 화면용 레이아웃 */
export default function AppLayout() {
  useScrollTopOnNavigate();

  return (
    <AppFrame>
      <Main>
        <Outlet />
      </Main>
      <BottomTabBar />
    </AppFrame>
  );
}

const Main = styled.main`
  /* 탭 바에 콘텐츠가 가려지지 않도록 */
  padding-bottom: calc(${({ theme }) => theme.layout.tabBarHeight} + env(safe-area-inset-bottom, 0px));
`;
