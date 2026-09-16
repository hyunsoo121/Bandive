import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppProvider, useApp } from './store/AppContext';
import { AppLayout } from './components/AppLayout';
import { AppChrome } from './components/AppChrome';
import { LoginModal } from './components/LoginModal';
import { HomePage } from './pages/HomePage';
import { SongsPage } from './pages/SongsPage';
import { SchedulePage } from './pages/SchedulePage';
import { MediaPage } from './pages/MediaPage';
import { MembersPage } from './pages/MembersPage';
import { FollowersPage } from './pages/FollowersPage';
import { BandSettingsPage } from './pages/BandSettingsPage';
import { ExplorePage } from './pages/ExplorePage';
import {
  HomeRedirect,
  InviteJoin,
  InviteShareRedirect,
  OAuthFailure,
  OAuthSuccess,
} from './pages/SystemPages';

/** 라우트와 무관하게 떠야 하는 모달 (랜딩·초대 페이지에서도 로그인 모달이 필요). */
function GlobalModals() {
  const { loginOpen } = useApp();
  return loginOpen ? <LoginModal /> : null;
}

export default function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/oauth/success" element={<OAuthSuccess />} />
          <Route path="/oauth/failure" element={<OAuthFailure />} />
          {/* 운영에선 Caddy 가 /invite/{code} 를 백엔드(OG 태그 HTML)로 먼저 보내 여기까지 안 옴.
              로컬 dev(프록시 없음) 등 SPA 로 직접 떨어지는 경우의 안전망 — 바로 /join 으로 넘긴다. */}
          <Route path="/invite/:code" element={<InviteShareRedirect />} />
          <Route path="/join/:code" element={<InviteJoin />} />
          <Route
            path="/explore"
            element={
              <AppChrome>
                <ExplorePage />
              </AppChrome>
            }
          />
          <Route path="/bands/:bandId" element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="songs" element={<SongsPage />} />
            <Route path="schedule" element={<SchedulePage />} />
            <Route path="media" element={<MediaPage />} />
            <Route path="members" element={<MembersPage />} />
            <Route path="followers" element={<FollowersPage />} />
            <Route path="settings" element={<BandSettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <GlobalModals />
      </AppProvider>
    </BrowserRouter>
  );
}
