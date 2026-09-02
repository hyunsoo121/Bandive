import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { startKakaoLogin } from '../api/auth';
import { BrandMark } from '../components/BrandMark';
import { CreateBandModal } from '../components/CreateBandModal';

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
  const { bootLoading, bands, user, openCreate, createOpen } = useApp();

  if (bootLoading) return <FullscreenLoader label="세션 확인 중…" />;
  if (bands.length > 0) return <Navigate to={`/bands/${bands[0].id}`} replace />;

  return (
    <CenterBox>
      <BrandMark size={40} wordmark />
      {user ? (
        <>
          <p className="muted">
            아직 속한 밴드가 없습니다. 밴드를 만들거나 초대 링크로 참여하세요.
          </p>
          <button type="button" className="btn btn--primary" onClick={openCreate}>
            새 밴드 만들기
          </button>
        </>
      ) : (
        <>
          <p className="muted">
            밴드를 만들고 멤버를 초대해 합주곡·일정·영상을 한곳에서 관리하세요.
          </p>
          <button type="button" className="btn btn--primary" onClick={startKakaoLogin}>
            카카오로 시작하기
          </button>
        </>
      )}
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
    return <Navigate to={`/invite/${pending}`} replace />;
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

/** "/invite/:code" — 로그인 상태면 바로 가입, 아니면 로그인 후 이 링크로 되돌아온다. */
export function InviteJoin() {
  const { code } = useParams();
  const { bootLoading, user, joinByInvite } = useApp();
  const navigate = useNavigate();
  const ran = useRef(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (bootLoading || !user || !code || ran.current) return;
    ran.current = true;
    joinByInvite(code)
      .then((band) => navigate(`/bands/${band.id}`, { replace: true }))
      .catch((e: unknown) =>
        setError(e instanceof Error ? e.message : '초대 코드로 가입하지 못했습니다.'),
      );
  }, [bootLoading, user, code, joinByInvite, navigate]);

  if (bootLoading) return <FullscreenLoader label="세션 확인 중…" />;

  if (!user) {
    return (
      <CenterBox>
        <BrandMark size={32} />
        <p className="muted">초대 링크로 참여하려면 로그인이 필요합니다.</p>
        <button
          type="button"
          className="btn btn--primary"
          onClick={() => {
            if (code) sessionStorage.setItem(PENDING_INVITE_KEY, code);
            startKakaoLogin();
          }}
        >
          카카오로 로그인
        </button>
      </CenterBox>
    );
  }

  if (error) {
    return (
      <CenterBox>
        <BrandMark size={32} />
        <p className="muted">{error}</p>
        <Link className="btn" to="/">
          홈으로
        </Link>
      </CenterBox>
    );
  }

  return <FullscreenLoader label="밴드에 참여하는 중…" />;
}
