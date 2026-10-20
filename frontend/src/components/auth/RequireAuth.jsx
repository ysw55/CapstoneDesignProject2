import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { ChevronLeft, LockKeyhole } from 'lucide-react';
import Button from '../common/Button';
import SocialLoginButtons from './SocialLoginButtons';
import { useAuth } from '../../context/AuthContext';

/**
 * 로그인이 필요한 화면(글쓰기, 책장 추가, 완독 인증 등)을 감싸는 가드.
 * 비로그인이면 안내 화면을 보여 주고, 로그인하면 그 자리에서 바로 원래 화면으로 바뀐다.
 */
export default function RequireAuth({ children, title = '로그인이 필요해요' }) {
  const { status, isLoggedIn, openLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  if (isLoggedIn) return children;

  const goBack = () => (location.key === 'default' ? navigate('/', { replace: true }) : navigate(-1));

  return (
    <Screen>
      <TopBar>
        <IconBtn type="button" aria-label="뒤로 가기" onClick={goBack}>
          <ChevronLeft size={26} strokeWidth={2} />
        </IconBtn>
      </TopBar>

      {status === 'restoring' ? (
        <Body aria-busy="true">
          <Muted>로그인 정보를 확인하고 있어요…</Muted>
        </Body>
      ) : (
        <Body>
          <IconCircle>
            <LockKeyhole size={28} strokeWidth={1.8} aria-hidden />
          </IconCircle>
          <Title>{title}</Title>
          <Muted>기록은 계정에 저장돼서 어느 기기에서든 이어서 볼 수 있어요.</Muted>
          <Actions>
            <Button type="button" $variant="primary" $block onClick={() => openLogin('login')}>
              이메일로 로그인
            </Button>
            <Button type="button" $variant="secondary" $block onClick={() => openLogin('signup')}>
              회원가입
            </Button>
            <SocialLoginButtons compact />
          </Actions>
        </Body>
      )}
    </Screen>
  );
}

const Screen = styled.div`
  min-height: 100vh;
  min-height: 100dvh;
`;

const TopBar = styled.header`
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 ${({ theme }) => theme.space(2)};
`;

const IconBtn = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;

const Body = styled.section`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: ${({ theme }) => `${theme.space(12)} ${theme.layout.gutter}`};
`;

const IconCircle = styled.div`
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: ${({ theme }) => theme.space(4)};
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const Title = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.xl};
  font-weight: 800;
  letter-spacing: -0.03em;
`;

const Muted = styled.p`
  max-width: 280px;
  margin-top: ${({ theme }) => theme.space(2)};
  font-size: 14px;
  line-height: 1.55;
  color: ${({ theme }) => theme.colors.textSecondary};
  word-break: keep-all;
`;

const Actions = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  width: 100%;
  margin-top: ${({ theme }) => theme.space(8)};
`;
