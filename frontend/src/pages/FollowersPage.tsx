import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import { VISIBILITY_LABEL } from '../lib/bandVisibility';
import './FollowersPage.css';

/** 팔로워 관리 — 관리자 전용. 홈의 "팔로워" 통계 / 사이드바 "팔로워" 에서 진입. */
export function FollowersPage() {
  const {
    currentBand,
    role,
    approvedFollowers,
    pendingFollowers,
    refreshFollowers,
    approveFollower,
    rejectFollower,
    removeFollower,
    updateBandVisibility,
  } = useApp();
  const [error, setError] = useState<string | null>(null);

  const isOwner = role === 'owner';
  const bandId = currentBand?.id;

  useEffect(() => {
    if (isOwner) void refreshFollowers();
  }, [isOwner, bandId, refreshFollowers]);

  if (!currentBand) return null;
  if (!isOwner) return <Navigate to={`/bands/${currentBand.id}`} replace />;

  const run = async (fn: () => Promise<unknown>, fail: string) => {
    setError(null);
    try {
      await fn();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : fail);
    }
  };

  // PRIVATE 만 팔로우 불가 — FOLLOWERS(승인제)·PUBLIC(즉시 승인) 은 둘 다 팔로우를 받는다.
  const notFollowable = currentBand.visibility === 'PRIVATE';

  return (
    <div className="followers">
      <header className="followers__head">
        <h2>팔로워</h2>
        <span className="muted" style={{ fontSize: 12 }}>
          승인 {approvedFollowers.length}명
          {pendingFollowers.length > 0 ? ` · 요청 ${pendingFollowers.length}건` : ''}
        </span>
      </header>

      {notFollowable && (
        <div className="followers__notice panel">
          <p style={{ fontSize: 12, margin: 0, lineHeight: 1.6 }}>
            지금 공개범위는 <strong>{VISIBILITY_LABEL[currentBand.visibility]}</strong> 라 팔로우를
            받지 않습니다. 팔로워 공개(승인 필요)나 전체공개(즉시 승인)로 바꾸면 다른 사용자가
            팔로우할 수 있어요.
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="btn btn--sm"
              onClick={() =>
                run(() => updateBandVisibility('FOLLOWERS'), '공개범위를 바꾸지 못했습니다.')
              }
            >
              팔로워 공개로 전환
            </button>
            <Link className="btn btn--ghost btn--sm" to={`/bands/${currentBand.id}/settings`}>
              밴드 설정에서 변경
            </Link>
          </div>
        </div>
      )}

      <section className="followers__section">
        <div className="spread">
          <span className="kicker">받은 요청</span>
          <span style={{ fontSize: 11, fontWeight: 700 }}>{pendingFollowers.length}건</span>
        </div>
        {pendingFollowers.length === 0 ? (
          <span className="muted" style={{ fontSize: 12 }}>
            대기 중인 요청이 없습니다.
          </span>
        ) : (
          pendingFollowers.map((f) => (
            <div key={f.userId} className="followers__row">
              <Avatar label={f.initial} size={28} color="var(--color-neutral-500)" />
              <span className="followers__name">{f.nickname}</span>
              <button
                type="button"
                className="followers__btn followers__btn--ok"
                onClick={() => run(() => approveFollower(f.userId), '승인하지 못했습니다.')}
              >
                승인
              </button>
              <button
                type="button"
                className="followers__btn"
                onClick={() => run(() => rejectFollower(f.userId), '거절하지 못했습니다.')}
              >
                거절
              </button>
            </div>
          ))
        )}
      </section>

      <section className="followers__section">
        <div className="spread">
          <span className="kicker">팔로워</span>
          <span style={{ fontSize: 11, fontWeight: 700 }}>{approvedFollowers.length}명</span>
        </div>
        {approvedFollowers.length === 0 ? (
          <span className="muted" style={{ fontSize: 12 }}>
            아직 승인된 팔로워가 없습니다.
          </span>
        ) : (
          approvedFollowers.map((f) => (
            <div key={f.userId} className="followers__row">
              <Avatar label={f.initial} size={28} color="var(--color-neutral-500)" />
              <span className="followers__name">{f.nickname}</span>
              <button
                type="button"
                className="followers__btn"
                onClick={() => run(() => removeFollower(f.userId), '팔로워를 내보내지 못했습니다.')}
              >
                내보내기
              </button>
            </div>
          ))
        )}
      </section>

      {error && <p className="followers__error">{error}</p>}
    </div>
  );
}
