import React from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ThemeModeProvider } from './context/ThemeModeContext';
import { AppDataProvider } from './context/AppDataContext';
import { AuthProvider } from './context/AuthContext';
import LoginSheetHost from './components/auth/LoginSheetHost';
import RequireAuth from './components/auth/RequireAuth';
import GlobalStyle from './styles/GlobalStyle';
import AppLayout from './components/layout/AppLayout';
import StackLayout from './components/layout/StackLayout';
import Settings from './components/Settings/Settings';
import CommunityPage from './pages/CommunityPage';
import RankingPage from './pages/RankingPage';
import BookshelfPage from './pages/BookshelfPage';
import ProfilePage from './pages/ProfilePage';
import BookFormPage from './pages/BookFormPage';
import PostFormPage from './pages/PostFormPage';
import CompletionFormPage from './pages/CompletionFormPage';

export default function App() {
  return (
    <ThemeModeProvider>
      <GlobalStyle />
      <AuthProvider>
        <AppDataProvider>
          <BrowserRouter>
            <Routes>
              {/* 하단 탭 바가 보이는 메인 화면들 */}
              <Route element={<AppLayout />}>
                <Route index element={<CommunityPage />} />
                <Route path="ranking" element={<RankingPage />} />
                <Route path="bookshelf" element={<BookshelfPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Route>

              {/* 탭 바가 없는 하위 화면(글쓰기, 책 상세 등)은 여기에 추가 */}
              <Route element={<StackLayout />}>
                {/* 커뮤니티 글쓰기 / 수정 */}
                <Route path="posts/new" element={<RequireAuth><PostFormPage /></RequireAuth>} />
                <Route path="posts/:postId/edit" element={<RequireAuth><PostFormPage /></RequireAuth>} />
                {/* 내 책장 등록 / 수정 */}
                <Route path="books/new" element={<RequireAuth><BookFormPage /></RequireAuth>} />
                <Route path="books/:bookId/edit" element={<RequireAuth><BookFormPage /></RequireAuth>} />
                {/* 완독 인증 (백엔드 연동, 로그인 필요) */}
                <Route path="books/certify" element={<RequireAuth title="완독 인증은 로그인 후 할 수 있어요"><CompletionFormPage /></RequireAuth>} />
                <Route path="profile/settings" element={<Settings />} />
              </Route>

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AppDataProvider>
        {/* 어느 화면에서든 openLogin() 으로 여는 로그인 시트 */}
        <LoginSheetHost />
      </AuthProvider>
    </ThemeModeProvider>
  );
}
