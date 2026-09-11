import type { ReactNode } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { EXPLORE_NAV, NAV_ITEMS, TAB_KEYS } from '../lib/nav';
import { BrandMark } from './BrandMark';
import { NavIcon } from './NavIcon';
import { Avatar } from './Avatar';
import { GuestBanner } from './GuestBanner';
import { BandSwitcher } from './BandSwitcher';
import { CreateBandModal } from './CreateBandModal';
import { ProfileModal } from './ProfileModal';
import './AppLayout.css';

const ROLE_LABEL: Record<string, string> = { owner: '관리자', member: '사용자', guest: '비회원' };

/**
 * 사이드바 + 모바일 바 + 하단 탭바 껍데기. 밴드 스코프 화면(AppLayout)과 탐색 화면이 공유한다.
 * 밴드가 없으면(비소속 유저가 탐색만 볼 때) 밴드 전환/탭 내비는 감춘다.
 */
export function AppChrome({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const {
    user,
    currentBand,
    bands,
    role,
    switcherOpen,
    createOpen,
    profileOpen,
    openSwitcher,
    openLogin,
    openProfile,
    closeProfile,
    logout,
  } = useApp();

  const shownBand = currentBand ?? bands[0] ?? null;
  const base = shownBand ? `/bands/${shownBand.id}` : null;
  const meName = user ? user.name : '게스트';
  const meInitial = user ? user.initial : '?';

  const cls = (variant: 'side' | 'tab') => (variant === 'side' ? 'sidenav__item' : 'tabbar__item');

  const bandLink = (
    variant: 'side' | 'tab',
    item: { key: string; label: string; to: string; d1: string; d2: string },
  ) => (
    <NavLink
      key={item.key}
      to={item.to ? `${base}/${item.to}` : base!}
      end={!item.to}
      className={({ isActive }) => `${cls(variant)}${isActive ? ' is-active' : ''}`}
    >
      <NavIcon d1={item.d1} d2={item.d2} size={variant === 'side' ? 19 : 20} />
      <span>{item.label}</span>
    </NavLink>
  );

  const exploreLink = (variant: 'side' | 'tab') => (
    <NavLink
      key="explore"
      to={EXPLORE_NAV.to}
      className={({ isActive }) => `${cls(variant)}${isActive ? ' is-active' : ''}`}
    >
      <NavIcon d1={EXPLORE_NAV.d1} d2={EXPLORE_NAV.d2} size={variant === 'side' ? 19 : 20} />
      <span>{EXPLORE_NAV.label}</span>
    </NavLink>
  );

  // 데스크탑 사이드바: 전체 + 설정 + 탐색.  모바일 하단탭: 홈·곡·일정·영상 + 탐색 (멤버/설정은 홈에서).
  const sideNav = base ? NAV_ITEMS.map((item) => bandLink('side', item)) : null;
  const tabNav = base
    ? [
        ...NAV_ITEMS.filter((i) => (TAB_KEYS as readonly string[]).includes(i.key)).map((item) =>
          bandLink('tab', item),
        ),
        exploreLink('tab'),
      ]
    : null;

  return (
    <div className="app">
      {/* 데스크탑 사이드바 */}
      <aside className="app__sidebar">
        <div className="sidebar__brand">
          <BrandMark size={22} wordmark />
        </div>

        {shownBand ? (
          <button type="button" className="sidebar__band" onClick={openSwitcher}>
            <Avatar
              label={shownBand.initial}
              src={shownBand.logoUrl}
              color="var(--color-accent)"
              heading
              size={34}
            />
            <span className="sidebar__band-text">
              <strong>{shownBand.name}</strong>
              <span className="muted">멤버 {shownBand.memberCount}명 · 밴드 전환</span>
            </span>
            <span className="sidebar__chev">▼</span>
          </button>
        ) : (
          <Link to="/" className="sidebar__band">
            <span className="sidebar__band-text">
              <strong>내 밴드 없음</strong>
              <span className="muted">홈으로</span>
            </span>
          </Link>
        )}

        <nav className="sidenav">
          {sideNav}
          {base && role === 'owner' && (
            <NavLink
              to={`${base}/followers`}
              className={({ isActive }) => `sidenav__item${isActive ? ' is-active' : ''}`}
            >
              <NavIcon
                d1="M16 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8"
                d2="M3 21c0-3.3 3-5 7-5m6.5 6 4.5-4.2-1.6-1.8-2.9 2.7-1.4-1.3L14 15z"
              />
              <span>팔로워</span>
            </NavLink>
          )}
          {base && role === 'owner' && (
            <NavLink
              to={`${base}/settings`}
              className={({ isActive }) => `sidenav__item${isActive ? ' is-active' : ''}`}
            >
              <NavIcon
                d1="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6"
                d2="M19.4 13a7.9 7.9 0 0 0 0-2l2-1.5-2-3.5-2.4 1a8 8 0 0 0-1.7-1l-.4-2.5h-4l-.4 2.5a8 8 0 0 0-1.7 1l-2.4-1-2 3.5L4.6 11a7.9 7.9 0 0 0 0 2l-2 1.5 2 3.5 2.4-1a8 8 0 0 0 1.7 1l.4 2.5h4l.4-2.5a8 8 0 0 0 1.7-1l2.4 1 2-3.5z"
              />
              <span>설정</span>
            </NavLink>
          )}
          {exploreLink('side')}
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
                {base ? ROLE_LABEL[role] : user ? '내 정보' : '비회원'}
                {base && user ? ' · 내 정보' : ''}
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
          {shownBand && (
            <button type="button" className="mobilebar__band" onClick={openSwitcher}>
              <Avatar
                label={shownBand.initial}
                src={shownBand.logoUrl}
                color="var(--color-accent)"
                heading
                size={26}
              />
              <span className="mobilebar__band-name">{shownBand.name}</span>
              <span className="muted" style={{ fontSize: 11, fontWeight: 700 }}>
                전환 ▼
              </span>
            </button>
          )}
        </header>

        <GuestBanner />

        <main className="app__content scr">{children}</main>

        {/* 모바일 하단 탭바 */}
        {tabNav && <nav className="app__tabbar">{tabNav}</nav>}
      </div>

      {switcherOpen && <BandSwitcher onNavigate={(id) => navigate(`/bands/${id}`)} />}
      {createOpen && <CreateBandModal />}
      {profileOpen && <ProfileModal onClose={closeProfile} />}
    </div>
  );
}
