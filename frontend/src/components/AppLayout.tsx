import { useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { NAV_ITEMS } from '../lib/nav';
import { BrandMark } from './BrandMark';
import { NavIcon } from './NavIcon';
import { Avatar } from './Avatar';
import { GuestBanner } from './GuestBanner';
// import { DevRoleBar } from './DevRoleBar'; // 잠깐 숨김 (아래 렌더도 주석)
import { BandSwitcher } from './BandSwitcher';
import { CreateBandModal } from './CreateBandModal';
import { ProfileModal } from './ProfileModal';
import { FullscreenLoader } from '../pages/SystemPages';
import './AppLayout.css';

const ROLE_LABEL: Record<string, string> = { owner: '관리자', member: '사용자', guest: '비회원' };

export function AppLayout() {
  const { bandId } = useParams();
  const navigate = useNavigate();
  const {
    user,
    currentBandId,
    currentBand,
    bandLoading,
    role,
    switcherOpen,
    createOpen,
    profileOpen,
    setCurrentBandId,
    openSwitcher,
    openLogin,
    openProfile,
    closeProfile,
    logout,
  } = useApp();

  // URL 의 밴드 → 컨텍스트 (뒤로가기 / 직접 URL 진입 / 밴드 전환 대응)
  useEffect(() => {
    if (bandId && bandId !== currentBandId) setCurrentBandId(bandId);
  }, [bandId, currentBandId, setCurrentBandId]);

  const meName = user ? user.name : '게스트';
  const meInitial = user ? user.initial : '?';

  if (!currentBand) {
    if (bandLoading || bandId !== currentBandId)
      return <FullscreenLoader label="밴드 불러오는 중…" />;
    return (
      <div
        style={{
          minHeight: '100dvh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 14,
          textAlign: 'center',
        }}
      >
        <BrandMark size={32} />
        <p className="muted">밴드를 찾을 수 없거나 접근할 수 없습니다.</p>
        <Link className="btn" to="/">
          홈으로
        </Link>
      </div>
    );
  }

  const base = `/bands/${currentBand.id}`;

  const navList = (variant: 'side' | 'tab') =>
    NAV_ITEMS.map((item) => (
      <NavLink
        key={item.key}
        to={item.to ? `${base}/${item.to}` : base}
        end={!item.to}
        className={({ isActive }) =>
          `${variant === 'side' ? 'sidenav__item' : 'tabbar__item'}${isActive ? ' is-active' : ''}`
        }
      >
        <NavIcon d1={item.d1} d2={item.d2} size={variant === 'side' ? 19 : 20} />
        <span>{item.label}</span>
      </NavLink>
    ));

  return (
    <div className="app">
      {/* 데스크탑 사이드바 */}
      <aside className="app__sidebar">
        <div className="sidebar__brand">
          <BrandMark size={22} wordmark />
        </div>

        <button type="button" className="sidebar__band" onClick={openSwitcher}>
          <Avatar
            label={currentBand.initial}
            src={currentBand.logoUrl}
            color="var(--color-accent)"
            heading
            size={34}
          />
          <span className="sidebar__band-text">
            <strong>{currentBand.name}</strong>
            <span className="muted">멤버 {currentBand.memberCount}명 · 밴드 전환</span>
          </span>
          <span className="sidebar__chev">▼</span>
        </button>

        <nav className="sidenav">
          {navList('side')}
          {role === 'owner' && (
            <NavLink
              to={`/bands/${currentBandId}/settings`}
              className={({ isActive }) => `sidenav__item${isActive ? ' is-active' : ''}`}
            >
              <NavIcon
                d1="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6"
                d2="M19.4 13a7.9 7.9 0 0 0 0-2l2-1.5-2-3.5-2.4 1a8 8 0 0 0-1.7-1l-.4-2.5h-4l-.4 2.5a8 8 0 0 0-1.7 1l-2.4-1-2 3.5L4.6 11a7.9 7.9 0 0 0 0 2l-2 1.5 2 3.5 2.4-1a8 8 0 0 0 1.7 1l.4 2.5h4l.4-2.5a8 8 0 0 0 1.7-1l2.4 1 2-3.5z"
              />
              <span>설정</span>
            </NavLink>
          )}
        </nav>

        <div className="sidebar__me">
          <button type="button" className="sidebar__me-open" onClick={openProfile} disabled={!user}>
            <Avatar
              label={meInitial}
              size={30}
              src={user?.avatarUrl}
              color={user ? 'var(--color-text)' : 'var(--color-neutral-500)'}
            />
            <span className="stack" style={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
              <strong style={{ fontSize: 13 }}>{meName}</strong>
              <span className="muted" style={{ fontSize: 11 }}>
                {ROLE_LABEL[role]}
                {user ? ' · 내 정보' : ''}
              </span>
            </span>
          </button>
          {user ? (
            <button type="button" className="sidebar__me-btn" onClick={logout}>
              로그아웃
            </button>
          ) : (
            <button type="button" className="sidebar__me-btn" onClick={openLogin}>
              로그인
            </button>
          )}
        </div>
      </aside>

      <div className="app__main">
        {/* 모바일 상단바 */}
        <header className="app__mobilebar">
          <div className="mobilebar__brand">
            <BrandMark size={18} wordmark />
          </div>
          <button type="button" className="mobilebar__band" onClick={openSwitcher}>
            <Avatar
              label={currentBand.initial}
              src={currentBand.logoUrl}
              color="var(--color-accent)"
              heading
              size={26}
            />
            <span className="mobilebar__band-name">{currentBand.name}</span>
            <span className="muted" style={{ fontSize: 11, fontWeight: 700 }}>
              전환 ▼
            </span>
          </button>
        </header>

        <GuestBanner />

        <main className="app__content scr">
          <Outlet />
        </main>

        {/* 모바일 하단 탭바 */}
        <nav className="app__tabbar">{navList('tab')}</nav>
      </div>

      {/* 개발용 역할 스위처 — 사이드바 '내 정보' 영역을 가려서 잠깐 숨김. 필요하면 아래 주석 해제. */}
      {/* <DevRoleBar /> */}

      {switcherOpen && <BandSwitcher onNavigate={(id) => navigate(`/bands/${id}`)} />}
      {createOpen && <CreateBandModal />}
      {profileOpen && <ProfileModal onClose={closeProfile} />}
    </div>
  );
}
