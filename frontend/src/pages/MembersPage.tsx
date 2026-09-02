import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import './MembersPage.css';

export function MembersPage() {
  const { currentBand, role, members, kickMember, invite, issueInviteCode } = useApp();
  const [copied, setCopied] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!currentBand) return null;

  const isOwner = role === 'owner';
  const bandMembers = members.filter((m) => m.bandId === currentBand.id);

  const runIssue = async () => {
    setIssuing(true);
    setError(null);
    try {
      await issueInviteCode();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '초대 코드를 발급하지 못했습니다.');
    } finally {
      setIssuing(false);
    }
  };

  const runKick = async (userId: string) => {
    setError(null);
    try {
      await kickMember(userId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '멤버를 추방하지 못했습니다.');
    }
  };

  const copyLink = async () => {
    if (!invite) return;
    try {
      await navigator.clipboard.writeText(invite.url);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1600);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className="members">
      <header className="members__head">
        <h2>멤버</h2>
        <span className="muted" style={{ fontSize: 12 }}>
          {bandMembers.length}명
        </span>
      </header>

      <div className="members__list">
        {bandMembers.map((m) => {
          const canKick = isOwner && m.role !== 'owner';
          return (
            <div key={m.id} className="members__row">
              <Avatar label={m.initial} size={34} color={m.avatarColor} />
              <span className="stack" style={{ flex: 1, minWidth: 0 }}>
                <strong style={{ fontSize: 14 }}>{m.name}</strong>
              </span>
              <span
                className="members__role"
                style={{
                  background:
                    m.role === 'owner' ? 'var(--color-accent)' : 'var(--color-neutral-200)',
                  color: m.role === 'owner' ? '#fff' : 'var(--color-neutral-800)',
                }}
              >
                {m.role === 'owner' ? '밴드장' : '사용자'}
              </span>
              {canKick && (
                <button type="button" className="members__kick" onClick={() => runKick(m.id)}>
                  추방
                </button>
              )}
            </div>
          );
        })}
      </div>

      {error && (
        <p style={{ fontSize: 12, margin: '10px 0 0', color: 'var(--color-accent)' }}>{error}</p>
      )}

      {isOwner && (
        <div className="members__invite panel">
          <span className="kicker">멤버 초대</span>
          {invite ? (
            <>
              <div className="members__invite-row">
                <span className="members__code">{invite.code}</span>
                <button type="button" className="btn btn--sm" disabled={issuing} onClick={runIssue}>
                  {issuing ? '발급 중…' : '코드 재발급'}
                </button>
                <button type="button" className="btn btn--sm btn--primary" onClick={copyLink}>
                  {copied ? '복사됨 ✓' : '링크 복사'}
                </button>
              </div>
              <span className="muted" style={{ fontSize: 11, wordBreak: 'break-all' }}>
                {invite.url}
              </span>
            </>
          ) : (
            <>
              <button
                type="button"
                className="btn btn--sm btn--primary"
                disabled={issuing}
                onClick={runIssue}
              >
                {issuing ? '발급 중…' : '초대 코드 발급'}
              </button>
              <span className="muted" style={{ fontSize: 11 }}>
                발급하면 코드와 초대 링크가 여기에 표시됩니다. 재발급 시 이전 코드는 폐기됩니다.
              </span>
            </>
          )}
        </div>
      )}

      {!isOwner && (
        <p className="members__note muted">멤버 초대와 추방은 밴드장만 할 수 있습니다.</p>
      )}
    </div>
  );
}
