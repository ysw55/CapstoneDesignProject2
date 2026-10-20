import React from 'react';
import styled, { keyframes } from 'styled-components';
import { Loader2 } from 'lucide-react';

/** 서버 데이터를 기다리는 동안 보여 주는 화면 / 영역 */
export default function LoadingScreen({ message = '불러오는 중…', inline = false }) {
  return (
    <Wrap $inline={inline} role="status" aria-live="polite">
      <Spinner size={24} strokeWidth={2.2} aria-hidden />
      <span>{message}</span>
    </Wrap>
  );
}

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

const Spinner = styled(Loader2)`
  color: ${({ theme }) => theme.colors.accent};
  animation: ${spin} 0.8s linear infinite;
`;

const Wrap = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.space(3)};
  min-height: ${({ $inline }) => ($inline ? '240px' : '100dvh')};
  font-size: 14px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;
