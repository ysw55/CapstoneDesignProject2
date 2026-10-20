import styled from 'styled-components';

/** 헤더 등에서 쓰는 44px 터치 영역의 아이콘 버튼 */
export const IconButton = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-right: -10px;
  border-radius: 50%;
  transition:
    background ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.92);
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;
