import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { ThemeProvider } from 'styled-components';
import { darkTheme, lightTheme } from '../styles/theme';

const STORAGE_KEY = 'bookcanvas:theme-mode';

const ThemeModeContext = createContext(null);

function getInitialMode() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'light' || saved === 'dark') return saved;
  } catch {
    // 웹뷰/시크릿 모드에서 localStorage 접근이 막힐 수 있음
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * 다크모드 상태 + styled-components ThemeProvider 를 한 번에 제공한다.
 * 프로필 > 설정 화면에서 useThemeMode().toggleMode() 로 전환한다.
 */
export function ThemeModeProvider({ children }) {
  const [mode, setMode] = useState(getInitialMode);
  const theme = mode === 'dark' ? darkTheme : lightTheme;

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      /* noop */
    }
    // 상태바(웹뷰/모바일 브라우저) 색상도 테마에 맞춘다
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme.colors.background);
  }, [mode, theme]);

  const toggleMode = useCallback(() => setMode((m) => (m === 'dark' ? 'light' : 'dark')), []);

  const value = useMemo(() => ({ mode, setMode, toggleMode }), [mode, toggleMode]);

  return (
    <ThemeModeContext.Provider value={value}>
      <ThemeProvider theme={theme}>{children}</ThemeProvider>
    </ThemeModeContext.Provider>
  );
}

export function useThemeMode() {
  const ctx = useContext(ThemeModeContext);
  if (!ctx) throw new Error('useThemeMode 는 ThemeModeProvider 안에서만 사용할 수 있습니다.');
  return ctx;
}
