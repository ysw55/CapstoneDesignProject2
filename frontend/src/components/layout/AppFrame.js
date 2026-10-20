import styled from 'styled-components';

/**
 * 모든 화면이 공유하는 앱 프레임.
 * - 모바일: 화면 전체를 채움
 * - 데스크톱: 430px 폭의 "폰 화면"처럼 가운데 정렬
 */
const AppFrame = styled.div`
  position: relative;
  width: 100%;
  max-width: ${({ theme }) => theme.layout.maxWidth};
  min-height: 100vh;
  min-height: 100dvh;
  margin: 0 auto;
  padding-top: env(safe-area-inset-top, 0px);
  background: ${({ theme }) => theme.colors.background};
  box-shadow: ${({ theme }) => theme.shadow.frame};
  transition: background ${({ theme }) => theme.motion.normal};
`;

export default AppFrame;
