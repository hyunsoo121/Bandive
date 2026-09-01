import type { Role } from '../types';
import { useApp } from '../store/AppContext';
import './DevRoleBar.css';

const ROLES: { key: Role; label: string }[] = [
  { key: 'owner', label: '밴드장' },
  { key: 'member', label: '사용자' },
  { key: 'guest', label: '비회원' },
];

/**
 * 백엔드(카카오 로그인) 붙기 전까지 owner/member/guest 화면을 미리 보기 위한 개발용 스위처.
 * 프로덕션 빌드에서는 렌더링되지 않는다.
 */
export function DevRoleBar() {
  const { role, setDevRole } = useApp();
  if (!import.meta.env.DEV) return null;

  return (
    <div className="devbar">
      <span className="devbar__label">DEV · 역할</span>
      <div className="seg">
        {ROLES.map((r) => (
          <button
            key={r.key}
            type="button"
            className={`seg__opt${role === r.key ? ' seg__opt--on' : ''}`}
            onClick={() => setDevRole(r.key)}
          >
            {r.label}
          </button>
        ))}
      </div>
      <button type="button" className="btn btn--sm btn--ghost" onClick={() => setDevRole(null)}>
        실제값
      </button>
    </div>
  );
}
