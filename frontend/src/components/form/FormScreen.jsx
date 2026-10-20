import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled, { css, keyframes } from 'styled-components';
import { AlertCircle, Loader2, X } from 'lucide-react';
import Button from '../common/Button';

/**
 * 전체 화면 입력 폼의 공통 틀: 상단 바(닫기 + 제목) / 입력 영역 / 하단 고정 저장 버튼
 * 책장 등록, 커뮤니티 글쓰기, 각각의 수정 화면이 모두 이 틀을 쓴다.
 */
export default function FormScreen({
  title,
  onSubmit,
  submitLabel = '저장하기',
  canSubmit,
  saving,
  hint,
  fallbackPath = '/',
  errorMessage, // 서버 오류 메시지 — 저장 버튼 바로 위에 표시
  children,
}) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleClose = () => {
    // 주소로 바로 들어와 뒤로 갈 곳이 없으면 fallbackPath 로
    if (location.key === 'default') navigate(fallbackPath, { replace: true });
    else navigate(-1);
  };

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (canSubmit && !saving) onSubmit();
      }}
      noValidate
    >
      <TopBar>
        <CloseButton type="button" aria-label="닫기" onClick={handleClose}>
          <X size={24} strokeWidth={2} />
        </CloseButton>
        <TopBarTitle>{title}</TopBarTitle>
        <span aria-hidden style={{ width: 44 }} />
      </TopBar>

      <Fields>{children}</Fields>

      <SubmitBar>
        {errorMessage && (
          <ErrorBanner role="alert">
            <AlertCircle size={16} strokeWidth={2.2} aria-hidden />
            {errorMessage}
          </ErrorBanner>
        )}
        <Button type="submit" $variant="primary" $block disabled={!canSubmit || saving}>
          {saving ? (
            <>
              <Spinner size={18} strokeWidth={2.4} aria-hidden />
              저장 중…
            </>
          ) : (
            submitLabel
          )}
        </Button>
        {!canSubmit && hint && <Hint>{hint}</Hint>}
      </SubmitBar>
    </Form>
  );
}


/* ───────── 입력 요소 (각 폼에서 import 해서 사용) ───────── */

export const Field = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  min-width: 0;
  margin: 0;
  padding: 0;
  border: none; /* as="fieldset" 일 때 기본 테두리 제거 */
`;

export const LabelRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
`;

export const Label = styled.label`
  padding: 0;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.02em;
`;

export const Counter = styled.span`
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: ${({ $full, theme }) => ($full ? theme.colors.danger : theme.colors.textTertiary)};
`;

const inputBase = css`
  width: 100%;
  padding: 14px 16px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font: inherit;
  font-size: 16px; /* iOS 는 16px 미만 입력란에 포커스하면 화면을 확대함 */
  color: ${({ theme }) => theme.colors.textPrimary};
  background: ${({ theme }) => theme.colors.backgroundAlt};
  outline: none;
  transition:
    border-color 150ms,
    background 150ms,
    box-shadow 150ms;

  &::placeholder {
    color: ${({ theme }) => theme.colors.textTertiary};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.accent};
    background: ${({ theme }) => theme.colors.surface};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.colors.accentSoft};
  }

  &:disabled {
    color: ${({ theme }) => theme.colors.textTertiary};
    cursor: not-allowed;
  }
`;

export const Input = styled.input`
  ${inputBase}
  height: 52px;
  appearance: none;
`;

export const Textarea = styled.textarea`
  ${inputBase}
  resize: none;
  font-family: ${({ theme }) => theme.font.serif};
  line-height: 1.7;
`;

/** 둘 중 하나를 고르는 세그먼트 컨트롤 (예: 책장에서 고르기 / 직접 입력) */
export const Segmented = styled.div`
  display: grid;
  grid-template-columns: repeat(${({ $count = 2 }) => $count}, 1fr);
  padding: 4px;
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceMuted};
`;

export const SegmentButton = styled.button`
  height: 40px;
  border-radius: 9px;
  font-size: 14px;
  font-weight: ${({ $active }) => ($active ? 700 : 500)};
  color: ${({ $active, theme }) => ($active ? theme.colors.textPrimary : theme.colors.textSecondary)};
  background: ${({ $active, theme }) => ($active ? theme.colors.surface : 'transparent')};
  box-shadow: ${({ $active }) => ($active ? '0 1px 3px rgba(17, 17, 17, 0.1)' : 'none')};
  transition:
    background ${({ theme }) => theme.motion.fast},
    color ${({ theme }) => theme.motion.fast};
`;

/* ───────── 틀 스타일 ───────── */

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

const Spinner = styled(Loader2)`
  animation: ${spin} 0.8s linear infinite;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
`;

const TopBar = styled.header`
  position: sticky;
  top: env(safe-area-inset-top, 0px);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 ${({ theme }) => theme.space(2)};
  background: ${({ theme }) => theme.colors.background};
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
`;

const CloseButton = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;

const TopBarTitle = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const Fields = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: ${({ theme }) => theme.space(6)};
  padding: ${({ theme }) => `${theme.space(6)} ${theme.layout.gutter}`};
`;

const SubmitBar = styled.div`
  position: sticky;
  bottom: 0;
  padding: ${({ theme }) => `${theme.space(3)} ${theme.layout.gutter}`};
  padding-bottom: calc(${({ theme }) => theme.space(3)} + env(safe-area-inset-bottom, 0px));
  background: ${({ theme }) => theme.colors.background};
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const ErrorBanner = styled.p`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: ${({ theme }) => theme.space(3)};
  padding: 12px 14px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};

  svg {
    flex-shrink: 0;
  }
`;

const Hint = styled.p`
  margin-top: ${({ theme }) => theme.space(2)};
  text-align: center;
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;
