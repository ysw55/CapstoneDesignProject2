import React from 'react';
import styled from 'styled-components';

/** 각 탭 상단의 큰 제목 헤더. 스크롤해도 상단에 붙어 있다. */
export default function ScreenHeader({ title, subtitle, right }) {
  return (
    <Header>
      <div>
        <Title>{title}</Title>
        {subtitle && <Subtitle>{subtitle}</Subtitle>}
      </div>
      {right && <Actions>{right}</Actions>}
    </Header>
  );
}

const Header = styled.header`
  position: sticky;
  top: env(safe-area-inset-top, 0px);
  z-index: 10;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: ${({ theme }) => `${theme.space(6)} ${theme.layout.gutter} ${theme.space(4)}`};
  background: ${({ theme }) => theme.colors.background};
  transition: background ${({ theme }) => theme.motion.normal};
`;

const Title = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.title};
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1.2;
`;

const Subtitle = styled.p`
  margin-top: 4px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const Actions = styled.div`
  display: flex;
  gap: 4px;
  color: ${({ theme }) => theme.colors.textPrimary};
`;
