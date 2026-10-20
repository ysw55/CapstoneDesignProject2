import React from 'react';
import { Outlet } from 'react-router-dom';
import styled from 'styled-components';
import AppFrame from './AppFrame';
import useScrollTopOnNavigate from './useScrollTopOnNavigate';

/** 탭 바 없이 위로 쌓이는 하위 화면(설정, 상세, 글쓰기 등)용 레이아웃 */
export default function StackLayout() {
  useScrollTopOnNavigate();

  return (
    <AppFrame>
      <Main>
        <Outlet />
      </Main>
    </AppFrame>
  );
}

const Main = styled.main`
  padding-bottom: env(safe-area-inset-bottom, 0px);
`;
