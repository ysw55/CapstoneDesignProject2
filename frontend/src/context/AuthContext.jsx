import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth';
import { UNAUTHORIZED_EVENT, tokenStorage } from '../api/client';

/**
 * 로그인 상태 전역 관리
 * - status: 'restoring'(저장된 토큰으로 내 정보 확인 중) | 'authenticated' | 'guest'
 * - 토큰은 api/client.js 의 tokenStorage(localStorage)에 저장되고, 요청마다 자동으로 헤더에 붙는다.
 * - openLogin(): 어느 화면에서든 로그인 시트를 띄운다 (App 의 LoginSheetHost 가 실제로 그림)
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState(() => (tokenStorage.get() ? 'restoring' : 'guest'));
  const [sessionExpired, setSessionExpired] = useState(false);
  const [loginSheet, setLoginSheet] = useState(null); // null | 'login' | 'signup'

  // 앱 시작 시 저장된 토큰이 있으면 내 정보를 불러와 로그인 상태 복원
  useEffect(() => {
    if (status !== 'restoring') return;
    let cancelled = false;

    authApi
      .fetchMe()
      .then((me) => {
        if (cancelled) return;
        setUser(me);
        setStatus('authenticated');
      })
      .catch(() => {
        // 401 이면 client.js 가 토큰을 이미 지움. 서버가 꺼져 있으면 토큰은 남겨 두고 다음 실행 때 재시도
        if (cancelled) return;
        setUser(null);
        setStatus('guest');
      });

    return () => {
      cancelled = true;
    };
  }, [status]);

  // 어떤 API 든 401(토큰 만료/위조)을 받으면 로그아웃 처리
  useEffect(() => {
    const onUnauthorized = () => {
      setUser(null);
      setStatus('guest');
      setSessionExpired(true);
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const handleTokenResponse = useCallback((res) => {
    tokenStorage.set(res.accessToken);
    setUser(res.user);
    setStatus('authenticated');
    setSessionExpired(false);
    return res.user;
  }, []);

  const login = useCallback((email, password) => authApi.login({ email, password }).then(handleTokenResponse), [handleTokenResponse]);

  const signup = useCallback(
    (email, password, username) => authApi.signup({ email, password, username }).then(handleTokenResponse),
    [handleTokenResponse]
  );

  /** 시연용 카카오/구글 간편 로그인 (실제 OAuth 아님) */
  const socialLogin = useCallback((provider) => authApi.demoLogin(provider).then(handleTokenResponse), [handleTokenResponse]);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
    setStatus('guest');
  }, []);

  const changePassword = useCallback(
    (currentPassword, newPassword) => authApi.changePassword({ currentPassword, newPassword }),
    []
  );

  /** 완독 인증 응답의 보상을 바로 반영 (내 정보 재조회 없이) */
  const applyCompletionReward = useCallback((reward) => {
    setUser((u) =>
      u && {
        ...u,
        profileBackgroundColor: reward.profileBackgroundColor,
        profileBorderColor: reward.profileBorderColor,
        rewardPoint: reward.totalPoint,
        completedBookCount: u.completedBookCount + 1,
      }
    );
  }, []);

  const openLogin = useCallback((mode = 'login') => {
    setSessionExpired(false);
    setLoginSheet(mode);
  }, []);
  const closeLogin = useCallback(() => setLoginSheet(null), []);

  const value = useMemo(
    () => ({
      user,
      status,
      isLoggedIn: status === 'authenticated',
      isSocialAccount: Boolean(user && user.provider !== 'LOCAL'),
      sessionExpired,
      dismissSessionExpired: () => setSessionExpired(false),
      login,
      signup,
      socialLogin,
      logout,
      changePassword,
      applyCompletionReward,
      loginSheet,
      openLogin,
      closeLogin,
    }),
    [user, status, sessionExpired, login, signup, socialLogin, logout, changePassword, applyCompletionReward, loginSheet, openLogin, closeLogin]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth 는 AuthProvider 안에서만 사용할 수 있습니다.');
  return ctx;
}
