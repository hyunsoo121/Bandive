import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import './MembersPage.css';

export function MembersPage() {
  const { currentBand, role, members, kickMember, inviteCodes, regenerateInviteCode } = useApp();
  const bandId = currentBand.id;
  const isOwner = role === 'owner';

  const bandMembers = members.filter((m) => m.bandId === bandId);
  const code = inviteCodes[bandId] ?? '—';
  const joinLink = `bandive.app/join/${code}`;

  const [copied, setCopied] = useState(false);

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(`https://${joinLink}`);
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
                <span className="muted" style={{ fontSize: 11 }}>
                  {m.part}
                </span>
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
                <button type="button" className="members__kick" onClick={() => kickMember(m.id)}>
                  추방
                </button>
              )}
            </div>
          );
        })}
      </div>

      {isOwner && (
        <div className="members__invite panel">
          <span className="kicker">멤버 초대</span>
          <div className="members__invite-row">
            <span className="members__code">{code}</span>
            <button
              type="button"
              className="btn btn--sm"
              onClick={() => regenerateInviteCode(bandId)}
            >
              코드 재발급
            </button>
            <button type="button" className="btn btn--sm btn--primary" onClick={copyLink}>
              {copied ? '복사됨 ✓' : '링크 복사'}
            </button>
          </div>
          <span className="muted" style={{ fontSize: 11 }}>
            {joinLink} · 7일 후 만료
          </span>
        </div>
      )}

      {!isOwner && (
        <p className="members__note muted">멤버 초대와 추방은 밴드장만 할 수 있습니다.</p>
      )}
    </div>
  );
}
