import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import { PartsPickerModal } from '../components/PartsPickerModal';
import type { Member } from '../types';
import './MembersPage.css';

export function MembersPage() {
  const {
    currentBand,
    role,
    user,
    members,
    kickMember,
    setMemberParts,
    setBandLeader,
    leaveBand,
    invite,
    issueInviteCode,
  } = useApp();
  const [copied, setCopied] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [leaveArmed, setLeaveArmed] = useState(false);
  const [leaving, setLeaving] = useState(false);

  if (!currentBand) return null;

  const isOwner = role === 'owner';
  const isMember = role === 'member';
  const bandMembers = members.filter((m) => m.bandId === currentBand.id);

  const runLeave = async () => {
    setLeaving(true);
    setError(null);
    try {
      await leaveBand();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '밴드를 탈퇴하지 못했습니다.');
      setLeaving(false);
      setLeaveArmed(false);
    }
  };

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

  const runSetParts = async (userId: string, parts: string[]) => {
    setError(null);
    try {
      await setMemberParts(userId, parts);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '세션을 저장하지 못했습니다.');
    }
  };

  const runSetLeader = async (userId: string | null) => {
    setError(null);
    try {
      await setBandLeader(userId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '리더를 지정하지 못했습니다.');
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
        {bandMembers.map((m) => (
          <MemberRow
            key={m.id}
            member={m}
            canEditParts={isOwner || m.id === user?.id}
            canManage={isOwner}
            onSetParts={(parts) => runSetParts(m.id, parts)}
            onSetLeader={(on) => runSetLeader(on ? m.id : null)}
            onKick={() => runKick(m.id)}
          />
        ))}
      </div>

      {error && (
        <p style={{ fontSize: 12, margin: '10px 16px 0', color: 'var(--color-accent)' }}>{error}</p>
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
        <p className="members__note muted">
          멤버 초대·추방과 리더 지정은 관리자만 할 수 있습니다. 내 세션은 직접 설정할 수 있어요.
        </p>
      )}

      {isMember && (
        <div className="members__leave">
          {leaveArmed ? (
            <>
              <button
                type="button"
                className="btn btn--sm members__leave-btn"
                disabled={leaving}
                onClick={runLeave}
              >
                {leaving ? '탈퇴 중…' : `정말 "${currentBand.name}" 탈퇴`}
              </button>
              <button type="button" className="btn btn--sm" onClick={() => setLeaveArmed(false)}>
                취소
              </button>
            </>
          ) : (
            <button
              type="button"
              className="btn btn--sm members__leave-btn"
              onClick={() => setLeaveArmed(true)}
            >
              밴드 탈퇴
            </button>
          )}
        </div>
      )}

      {isOwner && (
        <p className="members__leave members__leave--note muted">
          관리자는 밴드를 탈퇴할 수 없습니다. 다른 멤버에게 관리자를 위임한 뒤에 탈퇴하거나, 밴드
          설정에서 밴드를 삭제하세요.
        </p>
      )}
    </div>
  );
}

/* ───────────────────────── 멤버 행 ───────────────────────── */

interface RowProps {
  member: Member;
  canEditParts: boolean;
  canManage: boolean;
  onSetParts: (parts: string[]) => void;
  onSetLeader: (on: boolean) => void;
  onKick: () => void;
}

function MemberRow({ member, canEditParts, canManage, onSetParts, onSetLeader, onKick }: RowProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const parts = member.parts;

  return (
    <div className="members__row">
      <div className="members__row-top">
        <Avatar label={member.initial} size={34} color={member.avatarColor} />
        <span className="stack" style={{ flex: 1, minWidth: 0, gap: 4 }}>
          <strong style={{ fontSize: 14 }}>{member.name}</strong>
          <span className="members__parts-line">
            {parts.length ? (
              parts.map((p) => (
                <span key={p} className="members__part-chip">
                  {p}
                </span>
              ))
            ) : (
              <span className="muted" style={{ fontSize: 11 }}>
                세션 미지정
              </span>
            )}
            {canEditParts && (
              <button
                type="button"
                className="members__parts-edit"
                onClick={() => setPickerOpen(true)}
              >
                세션 설정
              </button>
            )}
          </span>
        </span>

        {member.leader && <span className="members__leader">리더</span>}
        <span
          className="members__role"
          style={{
            background:
              member.role === 'owner' ? 'var(--color-accent)' : 'var(--color-neutral-200)',
            color: member.role === 'owner' ? '#fff' : 'var(--color-neutral-800)',
          }}
        >
          {member.role === 'owner' ? '관리자' : '사용자'}
        </span>

        {canManage && (
          <button
            type="button"
            className={`members__leadbtn${member.leader ? ' is-on' : ''}`}
            onClick={() => onSetLeader(!member.leader)}
          >
            {member.leader ? '리더 해제' : '리더 지정'}
          </button>
        )}
        {canManage && member.role !== 'owner' && (
          <button type="button" className="members__kick" onClick={onKick}>
            추방
          </button>
        )}
      </div>

      {pickerOpen && (
        <PartsPickerModal
          name={member.name}
          current={parts}
          onSave={(next) => {
            onSetParts(next);
            setPickerOpen(false);
          }}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </div>
  );
}
