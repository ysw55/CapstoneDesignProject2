import React, { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import styled, { keyframes } from 'styled-components';

/**
 * 모달 공통 동작: body 스크롤 잠금, ESC 로 닫기, 열릴 때 첫 버튼에 포커스.
 * 웹뷰(특히 안드로이드)에서는 window.confirm 이 막혀 있을 수 있어 직접 만든 모달을 쓴다.
 */
function useModalBehavior(open, onClose, panelRef) {
  // onClose 가 매 렌더 새 함수여도 effect 가 다시 돌지 않도록 ref 로 보관
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return undefined;

    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    panelRef.current?.querySelector('button')?.focus({ preventScroll: true });

    const onKey = (e) => e.key === 'Escape' && onCloseRef.current();
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prevOverflow;
      window.removeEventListener('keydown', onKey);
    };
  }, [open, panelRef]);
}

/** 화면 아래에서 올라오는 시트 */
export function BottomSheet({ open, onClose, title, children }) {
  const panelRef = useRef(null);
  useModalBehavior(open, onClose, panelRef);
  if (!open) return null;

  return createPortal(
    <Backdrop onClick={onClose}>
      <SheetPanel
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <Grabber aria-hidden />
        {title && <SheetTitle>{title}</SheetTitle>}
        {children}
      </SheetPanel>
    </Backdrop>,
    document.body
  );
}

/** "정말 삭제할까요?" 같은 확인 창 */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = '확인',
  cancelLabel = '취소',
  tone = 'danger',
  onConfirm,
  onCancel,
}) {
  const panelRef = useRef(null);
  useModalBehavior(open, onCancel, panelRef);
  if (!open) return null;

  return createPortal(
    <Backdrop $center onClick={onCancel}>
      <DialogPanel
        ref={panelRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
        onClick={(e) => e.stopPropagation()}
      >
        <DialogTitle id="confirm-title">{title}</DialogTitle>
        {message && <DialogMessage id="confirm-message">{message}</DialogMessage>}
        <DialogActions>
          {/* 실수 방지를 위해 '취소'가 먼저 포커스를 받는다 */}
          <DialogButton type="button" onClick={onCancel}>
            {cancelLabel}
          </DialogButton>
          <DialogButton type="button" $tone={tone} onClick={onConfirm}>
            {confirmLabel}
          </DialogButton>
        </DialogActions>
      </DialogPanel>
    </Backdrop>,
    document.body
  );
}

const fade = keyframes`
  from { opacity: 0; }
  to   { opacity: 1; }
`;

const slideUp = keyframes`
  from { transform: translateY(100%); }
  to   { transform: translateY(0); }
`;

const zoomIn = keyframes`
  from { opacity: 0; transform: scale(0.94); }
  to   { opacity: 1; transform: scale(1); }
`;

const Backdrop = styled.div`
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: ${({ $center }) => ($center ? 'center' : 'flex-end')};
  justify-content: center;
  padding: ${({ $center }) => ($center ? '0 32px' : '0')};
  background: rgba(17, 17, 17, 0.45);
  animation: ${fade} 200ms ease-out;
`;

const SheetPanel = styled.div`
  width: 100%;
  max-width: ${({ theme }) => theme.layout.maxWidth};
  padding: ${({ theme }) => `${theme.space(2)} ${theme.layout.gutter}`};
  padding-bottom: calc(${({ theme }) => theme.space(6)} + env(safe-area-inset-bottom, 0px));
  max-height: calc(100dvh - 24px);
  overflow-y: auto;
  overscroll-behavior: contain;
  border-radius: 24px 24px 0 0;
  background: ${({ theme }) => theme.colors.surface};
  animation: ${slideUp} 320ms cubic-bezier(0.2, 0, 0, 1);
`;

const Grabber = styled.div`
  width: 36px;
  height: 5px;
  margin: 0 auto ${({ theme }) => theme.space(4)};
  border-radius: ${({ theme }) => theme.radius.pill};
  background: ${({ theme }) => theme.colors.switchOff};
`;

const SheetTitle = styled.h2`
  margin-bottom: ${({ theme }) => theme.space(4)};
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const DialogPanel = styled.div`
  width: 100%;
  max-width: 320px;
  padding: ${({ theme }) => `${theme.space(6)} ${theme.space(5)} ${theme.space(4)}`};
  border-radius: 20px;
  text-align: center;
  background: ${({ theme }) => theme.colors.surface};
  animation: ${zoomIn} 200ms cubic-bezier(0.2, 0, 0, 1);
`;

const DialogTitle = styled.h2`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const DialogMessage = styled.p`
  margin-top: ${({ theme }) => theme.space(2)};
  font-size: 14px;
  line-height: 1.55;
  color: ${({ theme }) => theme.colors.textSecondary};
  word-break: keep-all;
  white-space: pre-line;
`;

const DialogActions = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.space(2)};
  margin-top: ${({ theme }) => theme.space(6)};
`;

const DialogButton = styled.button`
  height: 48px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 700;
  color: ${({ $tone, theme }) =>
    $tone === 'danger' ? '#FFFFFF' : $tone === 'accent' ? theme.colors.onAccent : theme.colors.textPrimary};
  background: ${({ $tone, theme }) =>
    $tone === 'danger' ? theme.colors.danger : $tone === 'accent' ? theme.colors.accent : theme.colors.surfaceMuted};
  transition: transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.97);
  }
`;
