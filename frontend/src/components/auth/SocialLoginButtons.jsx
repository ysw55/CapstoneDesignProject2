import React, { useState } from 'react';
import styled, { keyframes } from 'styled-components';
import { Loader2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

/**
 * 카카오 / 구글 간편 로그인 버튼 (시연용)
 * 실제 OAuth 연동 전이라, 누르면 서버의 provider 별 시연 계정으로 로그인된다. (POST /api/auth/demo-login)
 */
export default function SocialLoginButtons({ onSuccess, compact = false }) {
  const { socialLogin } = useAuth();
  const [pending, setPending] = useState(null); // 'kakao' | 'google'
  const [error, setError] = useState(null);

  const handle = async (provider) => {
    setPending(provider);
    setError(null);
    try {
      const user = await socialLogin(provider);
      onSuccess?.(user);
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(null);
    }
  };

  return (
    <Wrap>
      <Row $compact={compact}>
        <SocialButton type="button" $brand="kakao" disabled={Boolean(pending)} onClick={() => handle('kakao')}>
          {pending === 'kakao' ? <Spinner size={18} aria-hidden /> : <KakaoLogo />}
          {compact ? '카카오' : '카카오로 계속하기'}
        </SocialButton>
        <SocialButton type="button" $brand="google" disabled={Boolean(pending)} onClick={() => handle('google')}>
          {pending === 'google' ? <Spinner size={18} aria-hidden /> : <GoogleLogo />}
          {compact ? 'Google' : 'Google로 계속하기'}
        </SocialButton>
      </Row>
      {error && <ErrorText role="alert">{error}</ErrorText>}
      <DemoNote>시연용 간편 로그인 · 실제 카카오/구글 계정과 연결되지 않아요</DemoNote>
    </Wrap>
  );
}

function KakaoLogo() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden>
      <path
        fill="currentColor"
        d="M12 3C6.48 3 2 6.54 2 10.9c0 2.8 1.86 5.27 4.66 6.67l-.95 3.5c-.08.3.26.55.52.38l4.15-2.75c.53.06 1.07.1 1.62.1 5.52 0 10-3.54 10-7.9S17.52 3 12 3Z"
      />
    </svg>
  );
}

function GoogleLogo() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden>
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
  );
}

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

const Spinner = styled(Loader2)`
  animation: ${spin} 0.8s linear infinite;
`;

const Wrap = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
`;

const Row = styled.div`
  display: grid;
  grid-template-columns: ${({ $compact }) => ($compact ? '1fr 1fr' : '1fr')};
  gap: ${({ theme }) => theme.space(2)};
`;

const BRAND = {
  kakao: { bg: '#FEE500', fg: 'rgba(0, 0, 0, 0.85)', border: '#FEE500' },
  google: { bg: '#FFFFFF', fg: '#1F1F1F', border: '#DADCE0' },
};

const SocialButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.space(2)};
  height: 50px;
  border: 1px solid ${({ $brand }) => BRAND[$brand].border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 600;
  letter-spacing: -0.02em;
  color: ${({ $brand }) => BRAND[$brand].fg};
  background: ${({ $brand }) => BRAND[$brand].bg};
  transition:
    transform ${({ theme }) => theme.motion.fast},
    filter ${({ theme }) => theme.motion.fast};

  &:active:not(:disabled) {
    transform: scale(0.98);
    filter: brightness(0.95);
  }

  &:disabled {
    opacity: 0.7;
    cursor: progress;
  }
`;

const ErrorText = styled.p`
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  color: ${({ theme }) => theme.colors.danger};
`;

const DemoNote = styled.p`
  font-size: 11.5px;
  text-align: center;
  color: ${({ theme }) => theme.colors.textTertiary};
`;
