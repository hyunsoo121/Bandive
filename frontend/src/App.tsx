import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppProvider } from './store/AppContext';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { SongsPage } from './pages/SongsPage';
import { SchedulePage } from './pages/SchedulePage';
import { MediaPage } from './pages/MediaPage';
import { MembersPage } from './pages/MembersPage';
import { HomeRedirect, InviteJoin, OAuthFailure, OAuthSuccess } from './pages/SystemPages';

export default function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/oauth/success" element={<OAuthSuccess />} />
          <Route path="/oauth/failure" element={<OAuthFailure />} />
          <Route path="/invite/:code" element={<InviteJoin />} />
          <Route path="/bands/:bandId" element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="songs" element={<SongsPage />} />
            <Route path="schedule" element={<SchedulePage />} />
            <Route path="media" element={<MediaPage />} />
            <Route path="members" element={<MembersPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppProvider>
    </BrowserRouter>
  );
}
