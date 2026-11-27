import React from 'react';
import styled from 'styled-components';
import { isLightColor } from '../../utils/format';

/**
 * 닉네임 첫 글자 아바타. 완독 보상으로 받은 스킨(배경색/테두리색)이 있으면 그 색으로 칠한다.
 */
export default function ProfileAvatar({ name = '', background, border, image, frame, size = 44 }) {
  const initial = name.trim().charAt(0).toUpperCase() || '?';
  const circle = (
    <Circle
      aria-hidden
      $size={size}
      $bg={background}
      $border={border}
      $frame={frame}
      $light={background ? isLightColor(background) : false}
    >
      {image ? <Photo src={image} alt="" /> : initial}
    </Circle>
  );
  // 그라데이션 링은 box-shadow 로 못 그리므로 바깥에 링 레이어를 두른다
  return RING_LAYERS[frame] ? (
    <RingWrap aria-hidden $size={size} $layer={RING_LAYERS[frame]}>
      {circle}
    </RingWrap>
  ) : (
    circle
  );
}

/* 상점에서 산 테두리 장식 — 색 테두리 대신 적용 */
const FRAMES = {
  DEMIAN: (theme) => `0 0 0 2px ${theme.colors.surface}`, // 바깥 링은 RingWrap 이 그림
  GOLD_RING: (theme) => `0 0 0 2px ${theme.colors.surface}, 0 0 0 5px #D4A017`,
};

/**
 * 『데미안』 — 싱클레어가 오가던 밝음과 어둠의 "두 세계"를 반으로 나뉜 금빛/남색 링으로,
 * 링 위쪽의 작은 금빛 점은 알을 깨고 나오는 새(아브락사스)를 닮은 빛을 나타낸다.
 */
const RING_LAYERS = {
  DEMIAN: {
    ring: 'conic-gradient(from 90deg, #E8C766 0 50%, #2B2D5B 50% 100%)',
    dot: '#E8C766',
    dotBorder: '#2B2D5B',
  },
};

/** 'BORDER_RRGGBB' = 단색 링, 그 밖은 FRAMES 의 장식. 알 수 없는 코드는 null */
function frameShadow(frame, theme) {
  if (!frame) return null;
  if (FRAMES[frame]) return FRAMES[frame](theme);
  const m = /^BORDER_([0-9A-Fa-f]{6})$/.exec(frame);
  return m ? `0 0 0 3px #${m[1]}` : null;
}

const Photo = styled.img`
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
`;

const Circle = styled.span`
  position: relative; /* 링 레이어(RingWrap ::before)보다 위에 그려지도록 */
  z-index: 1;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  overflow: hidden;
  width: ${({ $size }) => $size}px;
  height: ${({ $size }) => $size}px;
  border-radius: 50%;
  font-size: ${({ $size }) => Math.round($size * 0.4)}px;
  font-weight: 800;
  color: ${({ $bg, $light, theme }) => ($bg ? ($light ? '#212529' : '#FFFFFF') : theme.colors.accent)};
  background: ${({ $bg, theme }) => $bg ?? theme.colors.accentSoft};
  box-shadow: ${({ $frame, $border, theme }) => frameShadow($frame, theme) ?? ($border ? `0 0 0 3px ${$border}` : 'none')};
  transition:
    background ${({ theme }) => theme.motion.normal},
    box-shadow ${({ theme }) => theme.motion.normal};
`;

const RingWrap = styled.span`
  position: relative;
  display: inline-grid;
  flex-shrink: 0;

  &::before {
    content: '';
    position: absolute;
    inset: -5px;
    border-radius: 50%;
    background: ${({ $layer }) => $layer.ring};
    z-index: 0;
  }

  &::after {
    content: '';
    position: absolute;
    top: -5px;
    left: 50%;
    width: ${({ $size }) => Math.max(6, Math.round($size * 0.14))}px;
    height: ${({ $size }) => Math.max(6, Math.round($size * 0.14))}px;
    transform: translate(-50%, -30%);
    border: 1.5px solid ${({ $layer }) => $layer.dotBorder};
    border-radius: 50%;
    background: ${({ $layer }) => $layer.dot};
    z-index: 2;
  }
`;
