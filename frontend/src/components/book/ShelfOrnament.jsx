import React, { useId } from 'react';
import styled, { useTheme } from 'styled-components';

// 정사각 소용돌이 한 칸 (14 x 14): 바닥선 위에 올라앉은 그리스 키
const KEY_CELL = 'M0 13 H14 M1 13 V1 H13 V10 H4 V5 H9';

/**
 * 책장 상단 장식 띠에 새기는 그리스 키(meander) 무늬. 키스톤 양옆에 하나씩 놓는다.
 * side: 'left' | 'right'
 */
export default function ShelfOrnament({ side = 'left' }) {
  const theme = useTheme();
  const uid = useId().replace(/:/g, '');
  const { carving, carvingShadow } = theme.furniture;

  // 조각 느낌: 어두운 선을 살짝 아래에 한 번 더 그리고 그 위에 밝은 선
  const layers = [
    { id: `${uid}-s`, color: carvingShadow, dx: 0.8, dy: 0.9 },
    { id: `${uid}-l`, color: carving, dx: 0, dy: 0 },
  ];

  return (
    <Wrap $side={side} aria-hidden>
      <svg viewBox="0 0 140 26" width="100%" height="100%" preserveAspectRatio="xMidYMid meet">
        <defs>
          {layers.map(({ id, color }) => (
            <pattern key={id} id={id} width="14" height="14" patternUnits="userSpaceOnUse">
              <path d={KEY_CELL} fill="none" stroke={color} strokeWidth="1.3" strokeLinecap="square" />
            </pattern>
          ))}
        </defs>
        {layers.map(({ id, color, dx, dy }) => (
          <g key={id} transform={`translate(${dx} ${dy})`}>
            <path d="M0 3.5 H140 M0 22.5 H140" stroke={color} strokeWidth="1.3" fill="none" />
            <rect x="0" y="6" width="140" height="14" fill={`url(#${id})`} />
          </g>
        ))}
      </svg>
    </Wrap>
  );
}

const Wrap = styled.span`
  position: absolute;
  top: 6px;
  ${({ $side }) => ($side === 'left' ? 'left' : 'right')}: calc(26px + 6px);
  width: calc(50% - 26px - 11px - 12px); /* 기둥 머리와 키스톤 사이 */
  height: 26px;
  transform: ${({ $side }) => ($side === 'right' ? 'scaleX(-1)' : 'none')};
  pointer-events: none;

  svg {
    display: block;
    overflow: visible;
  }
`;
