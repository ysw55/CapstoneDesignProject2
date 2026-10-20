import React from 'react';
import styled, { keyframes } from 'styled-components';
import { CheckCircle2 } from 'lucide-react';

/** 탭 바 바로 위에 잠깐 떠오르는 알림. action = { label, onClick } */
export default function Toast({ message, action }) {
  return (
    <Wrap role="status" aria-live="polite">
      <CheckCircle2 size={18} strokeWidth={2.2} aria-hidden />
      <Message>{message}</Message>
      {action && (
        <ActionButton type="button" onClick={action.onClick}>
          {action.label}
        </ActionButton>
      )}
    </Wrap>
  );
}

const rise = keyframes`
  from { opacity: 0; transform: translate(-50%, 16px) scale(0.96); }
  to   { opacity: 1; transform: translate(-50%, 0) scale(1); }
`;

const Wrap = styled.div`
  position: fixed;
  z-index: 95;
  left: 50%;
  bottom: calc(${({ theme }) => theme.layout.tabBarHeight} + env(safe-area-inset-bottom, 0px) + 16px);
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(2)};
  width: calc(100% - 40px);
  max-width: calc(${({ theme }) => theme.layout.maxWidth} - 40px);
  padding: 0 ${({ theme }) => theme.space(2)} 0 ${({ theme }) => theme.space(4)};
  height: 52px;
  border-radius: ${({ theme }) => theme.radius.md};
  color: #ffffff;
  background: #111111;
  box-shadow: 0 10px 30px -10px rgba(17, 17, 17, 0.5);
  transform: translateX(-50%);
  animation: ${rise} 300ms cubic-bezier(0.2, 0, 0, 1);

  svg {
    flex-shrink: 0;
    color: #6cc4a1;
  }
`;

const Message = styled.p`
  flex: 1;
  font-size: 14px;
  font-weight: 600;
`;

const ActionButton = styled.button`
  flex-shrink: 0;
  height: 36px;
  padding: 0 ${({ theme }) => theme.space(3)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 14px;
  font-weight: 700;
  color: #6cc4a1;

  &:active {
    background: rgba(255, 255, 255, 0.1);
  }
`;
