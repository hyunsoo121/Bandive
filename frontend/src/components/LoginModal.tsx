import { useApp } from '../store/AppContext';
import { BrandMark } from './BrandMark';
import './LoginModal.css';

export function LoginModal() {
  const { login, closeLogin } = useApp();

  return (
    <div className="overlay overlay--center" onClick={closeLogin}>
      <div
        className="login-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <BrandMark size={36} />
        <h3>밴디브 로그인이 필요합니다</h3>
        <p className="muted">
          열람은 누구나 가능하지만 투표와 출결 체크는 밴드 멤버만 할 수 있습니다.
        </p>
        {/* 백엔드 붙기 전: 클릭 시 목 유저로 로그인 처리 */}
        <button type="button" className="login-modal__kakao" onClick={login}>
          카카오로 3초 만에 시작하기
        </button>
        <button type="button" className="btn btn--ghost btn--sm" onClick={closeLogin}>
          둘러보기 계속
        </button>
      </div>
    </div>
  );
}
