import React from 'react';
import styled from 'styled-components';

/**
 * iOS 스타일 토글 스위치
 * <Switch checked={on} onChange={setOn} label="다크 모드" />
 */
export default function Switch({ checked, onChange, label, disabled = false }) {
  return (
    <Track
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      $on={checked}
      onClick={(e) => {
        e.stopPropagation(); // 행 전체 클릭과 중복 실행 방지
        onChange(!checked);
      }}
    >
      <Knob $on={checked} />
    </Track>
  );
}

const TRACK_W = 51;
const TRACK_H = 31;
const KNOB = 27;
const PAD = 2;

const Track = styled.button`
  position: relative;
  flex-shrink: 0;
  width: ${TRACK_W}px;
  height: ${TRACK_H}px;
  border-radius: ${({ theme }) => theme.radius.pill};
  background: ${({ $on, theme }) => ($on ? theme.colors.accent : theme.colors.switchOff)};
  transition: background 250ms ease;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 2px;
  }
`;

const Knob = styled.span`
  position: absolute;
  top: ${PAD}px;
  left: ${PAD}px;
  width: ${KNOB}px;
  height: ${KNOB}px;
  border-radius: ${({ theme }) => theme.radius.pill};
  background: #fff;
  box-shadow:
    0 3px 8px rgba(0, 0, 0, 0.15),
    0 1px 1px rgba(0, 0, 0, 0.16);
  transform: translateX(${({ $on }) => ($on ? `${TRACK_W - KNOB - PAD * 2}px` : '0')});
  /* 살짝 튕기는 스프링 느낌 */
  transition:
    transform 300ms cubic-bezier(0.3, 1.4, 0.5, 1),
    width 200ms ease,
    margin 200ms ease;

  /* 누르고 있는 동안 노브가 옆으로 늘어나는 iOS 디테일 */
  ${Track}:active:not(:disabled) & {
    width: ${KNOB + 6}px;
    margin-left: ${({ $on }) => ($on ? '-6px' : '0')};
  }
`;
