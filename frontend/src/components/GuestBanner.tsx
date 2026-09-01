import { useApp } from '../store/AppContext';
import './GuestBanner.css';

export function GuestBanner() {
  const { role, openLogin } = useApp();
  if (role !== 'guest') return null;

  return (
    <button type="button" className="guest-banner" onClick={openLogin}>
      <span>비회원으로 열람 중 — 투표·출결은 로그인이 필요합니다</span>
      <span className="guest-banner__cta">로그인</span>
    </button>
  );
}
