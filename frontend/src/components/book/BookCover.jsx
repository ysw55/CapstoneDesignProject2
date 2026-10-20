import React from 'react';
import styled from 'styled-components';
import { isLightColor } from '../../utils/format';

/**
 * 표지 이미지가 없을 때 쓰는 단색 책 표지.
 * size: 'sm'(랭킹 리스트) | 'md'(책장) | 'lg'(1위 카드)
 */
export default function BookCover({ title, author, color, size = 'md' }) {
  return (
    <Cover $color={color} $light={isLightColor(color)} $size={size} aria-label={`${title} 표지`}>
      {size !== 'sm' && (
        <>
          <CoverTitle $size={size}>{title}</CoverTitle>
          {author && <CoverAuthor>{author}</CoverAuthor>}
        </>
      )}
    </Cover>
  );
}

const SIZES = {
  sm: { width: '40px', radius: '2px 4px 4px 2px' },
  md: { width: '100%', radius: '3px 6px 6px 3px' },
  lg: { width: '88px', radius: '3px 6px 6px 3px' },
};

const Cover = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex-shrink: 0;
  width: ${({ $size }) => SIZES[$size].width};
  aspect-ratio: 2 / 3;
  padding: ${({ $size }) => ($size === 'sm' ? 0 : '12px 10px 10px 14px')};
  overflow: hidden;
  border-radius: ${({ $size }) => SIZES[$size].radius};
  color: ${({ $light }) => ($light ? '#212529' : '#FFFFFF')};
  background: ${({ $color }) => $color};
  box-shadow:
    0 1px 2px rgba(17, 17, 17, 0.12),
    0 6px 14px -6px rgba(17, 17, 17, 0.25);

  /* 왼쪽 책등 접힘선 + 은은한 빛 반사 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      rgba(0, 0, 0, 0.18) 0,
      rgba(255, 255, 255, 0.12) 4%,
      rgba(0, 0, 0, 0.08) 7%,
      transparent 12%
    );
    pointer-events: none;
  }
`;

const CoverTitle = styled.p`
  font-family: ${({ theme }) => theme.font.serif};
  font-size: ${({ $size }) => ($size === 'lg' ? '13px' : '14px')};
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.02em;
  word-break: keep-all;
`;

const CoverAuthor = styled.p`
  font-size: 9px;
  line-height: 1.3;
  opacity: 0.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
