import React from 'react';
import AuthSheet from './AuthSheet';
import { useAuth } from '../../context/AuthContext';

/**
 * 앱 전체에서 하나만 두는 로그인 시트.
 * 어느 화면에서든 useAuth().openLogin() 을 부르면 여기서 열린다.
 */
export default function LoginSheetHost() {
  const { loginSheet, closeLogin } = useAuth();
  return <AuthSheet open={Boolean(loginSheet)} initialMode={loginSheet ?? 'login'} onClose={closeLogin} />;
}
