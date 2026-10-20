import React from 'react';
import styled from 'styled-components';
import { isLightColor } from '../../utils/format';

/**
 * 닉네임 첫 글자 아바타. 완독 보상으로 받은 스킨(배경색/테두리색)이 있으면 그 색으로 칠한다.
 */
export default function ProfileAvatar({ name = '', background, border, size = 44 }) {
  const initial = name.trim().charAt(0).toUpperCase() || '?';
  return (
    <Circle
      aria-hidden
      $size={size}
      $bg={background}
      $border={border}
      $light={background ? isLightColor(background) : false}
    >
      {initial}
    </Circle>
  );
}

const Circle = styled.span`
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: ${({ $size }) => $size}px;
  height: ${({ $size }) => $size}px;
  border-radius: 50%;
  font-size: ${({ $size }) => Math.round($size * 0.4)}px;
  font-weight: 800;
  color: ${({ $bg, $light, theme }) => ($bg ? ($light ? '#212529' : '#FFFFFF') : theme.colors.accent)};
  background: ${({ $bg, theme }) => $bg ?? theme.colors.accentSoft};
  box-shadow: ${({ $border }) => ($border ? `0 0 0 3px ${$border}` : 'none')};
  transition:
    background ${({ theme }) => theme.motion.normal},
    box-shadow ${({ theme }) => theme.motion.normal};
`;
