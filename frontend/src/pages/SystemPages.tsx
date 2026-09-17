import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { BrandMark } from '../components/BrandMark';
import { CreateBandModal } from '../components/CreateBandModal';
import { Avatar } from '../components/Avatar';
import { LandingPage } from './LandingPage';
import { fileUrl } from '../api/client';
import * as inviteApi from '../api/invites';
import type { InvitePreviewDto } from '../api/types';

const PENDING_INVITE_KEY = 'bandive.pendingInvite';

const boxStyle: React.CSSProperties = {
  minHeight: '100dvh',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 14,
  padding: 24,
  textAlign: 'center',
};

function CenterBox({ children }: { children: React.ReactNode }) {
  return <div style={boxStyle}>{children}</div>;
}

export function FullscreenLoader({ label = '불러오는 중…' }: { label?: string }) {
  return (
    <CenterBox>
      <BrandMark size={32} />
      <span className="muted">{label}</span>
    </CenterBox>
  );
}

/** "/" — 부팅 끝나면 첫 밴드로, 밴드가 없으면 로그인/생성 안내. */
export function HomeRedirect() {
  const { bootLoading, bands, user, openCreate, createOpen, openLogin } = useApp();

  if (bootLoading) return <FullscreenLoader label="세션 확인 중…" />;
  if (bands.length > 0) return <Navigate to={`/bands/${bands[0].id}`} replace />;

  // 로그인 전 방문자는 로그인 화면 대신 서비스 소개 랜딩페이지를 본다.
  if (!user) return <LandingPage onLogin={openLogin} />;

  return (
    <CenterBox>
      <BrandMark size={40} wordmark />
      <p className="muted">아직 속한 밴드가 없습니다. 밴드를 만들거나 초대 링크로 참여하세요.</p>
      <button type="button" className="btn btn--primary" onClick={openCreate}>
        새 밴드 만들기
      </button>
      <Link className="btn btn--ghost btn--sm" to="/explore">
        다른 밴드 구경하기
      </Link>
      {createOpen && <CreateBandModal />}
    </CenterBox>
  );
}

/** 카카오 로그인 성공 복귀 지점. 부팅(refresh)이 끝나면 목적지로 보낸다. */
export function OAuthSuccess() {
  const { bootLoading, bands } = useApp();

  if (bootLoading) return <FullscreenLoader label="로그인 처리 중…" />;

  const pending = sessionStorage.getItem(PENDING_INVITE_KEY);
  if (pending) {
    sessionStorage.removeItem(PENDING_INVITE_KEY);
    return <Navigate to={`/join/${pending}`} replace />;
  }
  return <Navigate to={bands.length > 0 ? `/bands/${bands[0].id}` : '/'} replace />;
}

export function OAuthFailure() {
  return (
    <CenterBox>
      <BrandMark size={32} />
      <p className="muted">로그인에 실패했습니다. 다시 시도해 주세요.</p>
      <Link className="btn" to="/">
        처음으로
      </Link>
    </CenterBox>
  );
}

/**
 * "/invite/:code" — 카카오톡 등 공유 링크 그대로. 운영에선 Caddy 가 이 경로를 백엔드(OG 태그 HTML)로 먼저
 * 보내니 SPA 까지 안 옴. 로컬 dev(프록시 없음)처럼 여기로 직접 떨어지는 경우의 안전망 — 바로 `/join/:code` 로.
 */
export function InviteShareRedirect() {
  const { code } = useParams();
  return <Navigate to={code ? `/join/${code}` : '/'} replace />;
}

/** "/band/:bandId" 로컬 dev 안전망(운영은 Caddy 가 OG HTML 로 먼저 처리) — 구경 화면으로 바로 넘긴다. */
export function BandShareRedirect() {
  const { bandId } = useParams();
  return <Navigate to={bandId ? `/explore/bands/${bandId}` : '/explore'} replace />;
}

/** "/join/:code" — 밴드 미리보기를 먼저 보여주고, "참여하기" 를 눌러야 가입한다 (오클릭 방지). */
export function InviteJoin() {
  const { code } = useParams();
  const { bootLoading, user, bands, joinByInvite, openLogin } = useApp();
  const navigate = useNavigate();

  const [preview, setPreview] = useState<InvitePreviewDto | null>(null);
  const [previewLoading, setPreviewLoading] = useState(true);
  const [previewError, setPreviewError] = useState<string | null>(null);

  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) return;
    let alive = true;
    setPreviewLoading(true);
    inviteApi
      .previewInvite(code)
      .then((dto) => {
        if (alive) setPreview(dto);
      })
      .catch((e: unknown) => {
        if (alive)
          setPreviewError(e instanceof Error ? e.message : '유효하지 않은 초대 링크입니다.');
      })
      .finally(() => {
        if (alive) setPreviewLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [code]);

  if (bootLoading || previewLoading) return <FullscreenLoader label="불러오는 중…" />;

  if (previewError || !preview) {
    return (
      <CenterBox>
        <BrandMark size={32} />
        <p className="muted">{previewError ?? '유효하지 않은 초대 링크입니다.'}</p>
        <Link className="btn" to="/">
          홈으로
        </Link>
      </CenterBox>
    );
  }

  const alreadyMember = bands.some((b) => b.id === String(preview.bandId));
  // 전체공개 밴드만 — 가입 없이도 콘텐츠를 볼 수 있으니 둘러보기 버튼을 보여준다.
  const browseButton = preview.visibility === 'PUBLIC' && (
    <Link className="btn btn--ghost btn--sm" to={`/explore/bands/${preview.bandId}`}>
      가입 전에 둘러보기
    </Link>
  );

  const previewCard = (
    <div
      className="panel"
      style={{
        padding: 24,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 10,
        minWidth: 260,
      }}
    >
      <Avatar
        label={[...preview.bandName][0] ?? '밴'}
        size={56}
        src={preview.logoUrl ? fileUrl(preview.logoUrl) : null}
        heading
        color="var(--color-accent)"
      />
      <strong style={{ fontFamily: 'var(--font-heading)', fontSize: 18 }}>
        {preview.bandName}
      </strong>
      {preview.description && (
        <p className="muted" style={{ margin: 0, textAlign: 'center' }}>
          {preview.description}
        </p>
      )}
      <span className="muted" style={{ fontSize: 12 }}>
        멤버 {preview.memberCount}명
      </span>
    </div>
  );

  if (!user) {
    return (
      <CenterBox>
        <BrandMark size={32} />
        {previewCard}
        <button
          type="button"
          className="btn btn--primary"
          onClick={() => {
            // 카카오는 전체 리다이렉트라 코드를 세션에 보관했다가 복귀 후 이어감. 이메일 로그인은 이 페이지에서 바로 이어짐.
            if (code) sessionStorage.setItem(PENDING_INVITE_KEY, code);
            openLogin();
          }}
        >
          로그인하고 참여하기
        </button>
        {browseButton}
      </CenterBox>
    );
  }

  if (alreadyMember) {
    return (
      <CenterBox>
        <BrandMark size={32} />
        {previewCard}
        <p className="muted">이미 이 밴드의 멤버입니다.</p>
        <button
          type="button"
          className="btn btn--primary"
          onClick={() => navigate(`/bands/${preview.bandId}`, { replace: true })}
        >
          밴드로 이동
        </button>
      </CenterBox>
    );
  }

  const handleJoin = async () => {
    if (!code || joining) return;
    setJoining(true);
    setJoinError(null);
    try {
      const band = await joinByInvite(code);
      navigate(`/bands/${band.id}`, { replace: true });
    } catch (e) {
      setJoinError(e instanceof Error ? e.message : '참여하지 못했습니다.');
      setJoining(false);
    }
  };

  return (
    <CenterBox>
      <BrandMark size={32} />
      {previewCard}
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="button" className="btn btn--primary" disabled={joining} onClick={handleJoin}>
          {joining ? '참여하는 중…' : '참여하기'}
        </button>
        <button
          type="button"
          className="btn btn--ghost"
          disabled={joining}
          onClick={() => navigate('/')}
        >
          취소
        </button>
      </div>
      {browseButton}
      {joinError && <p className="muted">{joinError}</p>}
    </CenterBox>
  );
}
