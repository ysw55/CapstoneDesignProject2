import React from 'react';
import styled from 'styled-components';
import { BadgeCheck } from 'lucide-react';
import { isLightColor } from '../../utils/format';

// 책마다 두께·키가 조금씩 달라 보이도록 id 로 고정된 값을 뽑는다 (새로고침해도 같은 모양)
const WIDTHS = [30, 34, 38, 32, 36, 40, 33];
const HEIGHTS = [168, 180, 160, 188, 172, 164, 184];

/**
 * 책장에 꽂힌 책의 옆면(책등). 제목은 세로로 쓴다.
 * 정면 표지는 BookCover 를 쓴다.
 */
export default function BookSpine({ title, author, color, seed = 0, certified = false }) {
  const n = Math.abs(Number(seed) || 0);
  return (
    <Spine
      $color={color}
      $light={isLightColor(color)}
      $width={WIDTHS[n % WIDTHS.length]}
      $height={HEIGHTS[(n * 3 + 1) % HEIGHTS.length]}
      aria-label={`${title} 책등`}
    >
      <SpineTitle>{title}</SpineTitle>
      {certified && (
        <Mark aria-label="완독 인증 완료">
          <BadgeCheck size={13} strokeWidth={2.6} aria-hidden />
        </Mark>
      )}
      {author && <SpineAuthor>{author}</SpineAuthor>}
    </Spine>
  );
}

const Spine = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: ${({ $width }) => $width}px;
  height: ${({ $height }) => $height}px;
  padding: 14px 0 12px;
  overflow: hidden;
  border-radius: 2px 2px 1px 1px;
  color: ${({ $light }) => ($light ? '#212529' : '#FFFFFF')};
  background: ${({ $color }) => $color};
  box-shadow:
    1px 0 0 rgba(0, 0, 0, 0.25),
    0 -1px 2px rgba(0, 0, 0, 0.2);

  /* 둥근 책등의 음영 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      rgba(0, 0, 0, 0.22) 0,
      rgba(255, 255, 255, 0.14) 22%,
      transparent 52%,
      rgba(0, 0, 0, 0.2) 100%
    );
    pointer-events: none;
  }

  /* 책등 위·아래 띠 */
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0.45;
    background:
      linear-gradient(currentColor, currentColor) top 7px left 4px / calc(100% - 8px) 1px no-repeat,
      linear-gradient(currentColor, currentColor) top 10px left 4px / calc(100% - 8px) 1px no-repeat,
      linear-gradient(currentColor, currentColor) bottom 7px left 4px / calc(100% - 8px) 1px no-repeat;
    pointer-events: none;
  }
`;

const SpineTitle = styled.p`
  flex: 1;
  min-height: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  writing-mode: vertical-rl;
  font-family: ${({ theme }) => theme.font.serif};
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.02em;
`;

const SpineAuthor = styled.p`
  margin-top: 6px;
  max-height: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  writing-mode: vertical-rl;
  font-size: 9px;
  opacity: 0.75;
`;

/* 제목 바로 아래(저자명 위)에 놓인다 */
const Mark = styled.span`
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  margin-top: 6px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.furniture.trim};
`;
