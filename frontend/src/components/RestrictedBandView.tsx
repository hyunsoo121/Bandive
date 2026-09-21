import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from './Avatar';
import { BrandMark } from './BrandMark';
import { VISIBILITY_HINT } from '../lib/bandVisibility';
import './RestrictedBandView.css';

/** 밴드 표지는 보이지만 콘텐츠는 게이트된 화면 (FOLLOWERS 밴드에 팔로우 안 함 / 승인 대기). */
export function RestrictedBandView() {
  const { currentBand, bands, user, openLogin, requestFollow, cancelFollow } = useApp();
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  if (!currentBand) return null;

  const run = async (fn: () => Promise<void>, fail: string) => {
    setBusy(true);
    setErr(null);
    try {
      await fn();
    } catch (e) {
      setErr(e instanceof Error ? e.message : fail);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="restricted">
      <div className="restricted__card panel">
        <Avatar
          label={currentBand.initial}
          size={56}
          src={currentBand.logoUrl}
          color="var(--color-neutral-700)"
        />
        <strong className="restricted__name">{currentBand.name}</strong>
        {currentBand.note && <p className="restricted__note muted">{currentBand.note}</p>}
        <span className="restricted__count muted">멤버 {currentBand.memberCount}명</span>

        <p className="restricted__hint">{VISIBILITY_HINT[currentBand.visibility]}</p>

        {!user ? (
          <button type="button" className="btn btn--primary" onClick={openLogin}>
            로그인하고 팔로우 요청
          </button>
        ) : currentBand.myRelation === 'PENDING' ? (
          <div className="restricted__actions">
            <span className="restricted__pending">팔로우 요청 대기 중</span>
            <button
              type="button"
              className="btn btn--sm"
              disabled={busy}
              onClick={() => run(cancelFollow, '요청을 취소하지 못했습니다.')}
            >
              요청 취소
            </button>
          </div>
        ) : (
          <button
            type="button"
            className="btn btn--primary"
            disabled={busy}
            onClick={() => run(requestFollow, '팔로우 요청에 실패했습니다.')}
          >
            {busy ? '요청 중…' : '팔로우 요청'}
          </button>
        )}
        {err && <span className="restricted__err">{err}</span>}

        <div className="restricted__back">
          <Link className="btn btn--sm" to="/explore">
            ← 탐색으로
          </Link>
          {bands.length > 0 ? (
            <Link className="btn btn--sm" to={`/bands/${bands[0].id}`}>
              내 밴드로 →
            </Link>
          ) : (
            <Link className="btn btn--sm" to="/">
              홈으로
            </Link>
          )}
        </div>
      </div>

      <div className="restricted__brand">
        <BrandMark size={20} />
      </div>
    </div>
  );
}
