import styled, { css } from 'styled-components';

/**
 * 공용 버튼
 * - primary  : 딥그린 채움. '기록 저장하기' 등 화면당 하나의 주요 행동
 * - secondary: 흰 바탕 + 테두리. 보조 행동
 * <Button $variant="primary" $block>기록 저장하기</Button>
 */
const variants = {
  primary: css`
    color: ${({ theme }) => theme.colors.onAccent};
    background: ${({ theme }) => theme.colors.accent};
    border-color: ${({ theme }) => theme.colors.accent};

    &:active:not(:disabled) {
      background: ${({ theme }) => theme.colors.accentPressed};
    }
  `,
  secondary: css`
    color: ${({ theme }) => theme.colors.textPrimary};
    background: ${({ theme }) => theme.colors.surface};
    border-color: ${({ theme }) => theme.colors.border};

    &:active:not(:disabled) {
      background: ${({ theme }) => theme.colors.surfaceMuted};
    }
  `,
};

const Button = styled.button`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.space(2)};
  width: ${({ $block }) => ($block ? '100%' : 'auto')};
  height: 52px;
  padding: 0 ${({ theme }) => theme.space(6)};
  border: 1px solid transparent;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 700;
  letter-spacing: -0.02em;
  transition:
    background ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  ${({ $variant = 'primary' }) => variants[$variant]}

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 2px;
  }
`;

export default Button;
