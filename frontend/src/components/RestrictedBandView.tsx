import { Link } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from './Avatar';
import { BrandMark } from './BrandMark';
import { VISIBILITY_HINT } from '../lib/bandVisibility';
import './RestrictedBandView.css';

/**
 * 밴드 표지는 보이지만 콘텐츠는 게이트된 화면 (주로 FOLLOWERS 밴드에 팔로우 안 한 경우).
 * 팔로우 요청 버튼은 P2 에서 붙는다.
 */
export function RestrictedBandView() {
  const { currentBand, user, openLogin } = useApp();
  if (!currentBand) return null;

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

        {currentBand.myRelation === 'PENDING' ? (
          <span className="restricted__pending">팔로우 요청 대기 중</span>
        ) : !user ? (
          <button type="button" className="btn btn--primary" onClick={openLogin}>
            로그인하고 팔로우 요청
          </button>
        ) : (
          <span className="muted" style={{ fontSize: 12 }}>
            팔로우 요청 기능은 곧 제공됩니다.
          </span>
        )}

        <Link className="btn btn--sm" to="/">
          홈으로
        </Link>
      </div>

      <div className="restricted__brand">
        <BrandMark size={20} />
      </div>
    </div>
  );
}
