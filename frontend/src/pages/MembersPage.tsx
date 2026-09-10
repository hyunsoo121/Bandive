import { useEffect, useState } from 'react';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import { PartsPickerModal } from '../components/PartsPickerModal';
import { DangerConfirmModal } from '../components/DangerConfirmModal';
import { UserProfileModal } from '../components/UserProfileModal';
import { PromptModal } from '../components/PromptModal';
import { GuestSessionModal } from '../components/GuestSessionModal';
import type { Guest, Member } from '../types';
import './MembersPage.css';

const GUESTS_OPEN_KEY = 'bandive:members:guestsOpen';

export function MembersPage() {
  const {
    currentBand,
    role,
    user,
    members,
    guests,
    kickMember,
    setMemberParts,
    setBandLeader,
    addGuest,
    renameGuest,
    setGuestSession,
    removeGuest,
    leaveBand,
    invite,
    issueInviteCode,
    pendingFollowers,
    refreshFollowers,
    approveFollower,
    rejectFollower,
  } = useApp();
  const [copied, setCopied] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [profileUserId, setProfileUserId] = useState<string | null>(null);

  useEffect(() => {
    if (role === 'owner') void refreshFollowers();
  }, [role, currentBand?.id, refreshFollowers]);

  if (!currentBand) return null;

  const isOwner = role === 'owner';
  const isMember = role === 'member';
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

  const runGuest = async (fn: () => Promise<unknown>, fail: string) => {
    setError(null);
    try {
      await fn();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : fail);
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
            onOpenProfile={() => setProfileUserId(m.id)}
            onSetParts={(parts) => runSetParts(m.id, parts)}
            onSetLeader={(on) => runSetLeader(on ? m.id : null)}
            onKick={() => kickMember(m.id)}
          />
        ))}
      </div>

      <GuestSection
        guests={guests}
        canManage={isOwner}
        onAdd={(name) => runGuest(() => addGuest(name), '게스트를 추가하지 못했습니다.')}
        onRename={(id, name) => runGuest(() => renameGuest(id, name), '이름을 바꾸지 못했습니다.')}
        onSetSession={(id, s) =>
          runGuest(() => setGuestSession(id, s), '세션을 저장하지 못했습니다.')
        }
        onRemove={(id) => runGuest(() => removeGuest(id), '게스트를 삭제하지 못했습니다.')}
      />

      {isOwner && (currentBand.visibility === 'FOLLOWERS' || pendingFollowers.length > 0) && (
        <div className="members__follows">
          <div className="spread">
            <span className="kicker">팔로우 요청</span>
            <span style={{ fontSize: 11, fontWeight: 700 }}>{pendingFollowers.length}건</span>
          </div>
          {pendingFollowers.length === 0 ? (
            <span className="muted" style={{ fontSize: 12 }}>
              대기 중인 요청이 없습니다.
            </span>
          ) : (
            pendingFollowers.map((f) => (
              <div key={f.userId} className="members__follow-row">
                <Avatar label={f.initial} size={26} color="var(--color-neutral-500)" />
                <span style={{ flex: 1, minWidth: 0, fontSize: 13, fontWeight: 600 }}>
                  {f.nickname}
                </span>
                <button
                  type="button"
                  className="members__follow-btn members__follow-btn--ok"
                  onClick={() => runGuest(() => approveFollower(f.userId), '승인하지 못했습니다.')}
                >
                  승인
                </button>
                <button
                  type="button"
                  className="members__follow-btn"
                  onClick={() => runGuest(() => rejectFollower(f.userId), '거절하지 못했습니다.')}
                >
                  거절
                </button>
              </div>
            ))
          )}
        </div>
      )}

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
          <button
            type="button"
            className="btn btn--sm members__leave-btn"
            onClick={() => setLeaveOpen(true)}
          >
            밴드 탈퇴
          </button>
        </div>
      )}

      {isOwner && (
        <p className="members__leave members__leave--note muted">
          관리자는 밴드를 탈퇴할 수 없습니다. 다른 멤버에게 관리자를 위임한 뒤에 탈퇴하거나, 밴드
          설정에서 밴드를 삭제하세요.
        </p>
      )}

      {leaveOpen && (
        <DangerConfirmModal
          title="밴드 탈퇴"
          message={
            <>
              정말 <strong>{currentBand.name}</strong> 에서 탈퇴하시겠습니까? 다시 들어오려면 초대
              코드가 필요합니다.
            </>
          }
          confirmPhrase="밴드 탈퇴"
          onConfirm={leaveBand}
          onClose={() => setLeaveOpen(false)}
        />
      )}

      {profileUserId && (
        <UserProfileModal userId={profileUserId} onClose={() => setProfileUserId(null)} />
      )}
    </div>
  );
}

/* ───────────────────────── 멤버 행 ───────────────────────── */

interface RowProps {
  member: Member;
  canEditParts: boolean;
  canManage: boolean;
  onOpenProfile: () => void;
  onSetParts: (parts: string[]) => void;
  onSetLeader: (on: boolean) => void;
  onKick: () => Promise<void>;
}

function MemberRow({
  member,
  canEditParts,
  canManage,
  onOpenProfile,
  onSetParts,
  onSetLeader,
  onKick,
}: RowProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const [kickOpen, setKickOpen] = useState(false);
  const parts = member.parts;

  return (
    <div className="members__row">
      <div className="members__row-top">
        <button
          type="button"
          className="members__profile-btn"
          onClick={onOpenProfile}
          aria-label={`${member.name} 프로필`}
        >
          <Avatar
            label={member.initial}
            size={34}
            src={member.avatarUrl}
            color={member.avatarColor}
          />
        </button>
        <span className="stack" style={{ flex: 1, minWidth: 0, gap: 4 }}>
          <button type="button" className="members__name-btn" onClick={onOpenProfile}>
            {member.name}
          </button>
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
          <button type="button" className="members__kick" onClick={() => setKickOpen(true)}>
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

      {kickOpen && (
        <DangerConfirmModal
          title="멤버 추방"
          message={
            <>
              <strong>{member.name}</strong> 님을 밴드에서 추방하시겠습니까? 이 멤버의 세션 설정도
              함께 사라집니다.
            </>
          }
          confirmPhrase="멤버 추방"
          onConfirm={onKick}
          onClose={() => setKickOpen(false)}
        />
      )}
    </div>
  );
}

/* ─────────────────────── 게스트 멤버 ─────────────────────── */

interface GuestSectionProps {
  guests: Guest[];
  canManage: boolean;
  onAdd: (name: string) => void;
  onRename: (id: string, name: string) => void;
  onSetSession: (id: string, session: string | null) => void;
  onRemove: (id: string) => void;
}

function GuestSection({
  guests,
  canManage,
  onAdd,
  onRename,
  onSetSession,
  onRemove,
}: GuestSectionProps) {
  const [open, setOpen] = useState(() => {
    try {
      return localStorage.getItem(GUESTS_OPEN_KEY) === '1';
    } catch {
      return false;
    }
  });
  const [prompt, setPrompt] = useState<{ mode: 'add' } | { mode: 'rename'; guest: Guest } | null>(
    null,
  );
  const [sessionFor, setSessionFor] = useState<Guest | null>(null);

  const toggle = () => {
    setOpen((v) => {
      const next = !v;
      try {
        localStorage.setItem(GUESTS_OPEN_KEY, next ? '1' : '0');
      } catch {
        /* 저장 불가 — 무시 */
      }
      return next;
    });
  };

  // 관리자도 아니고 게스트도 없으면 섹션 자체를 숨긴다
  if (!canManage && guests.length === 0) return null;

  return (
    <div className="members__guests">
      <button type="button" className="members__guests-head" onClick={toggle}>
        <span>게스트 {guests.length}명</span>
        <span className="members__guests-chev">{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div className="members__guests-body">
          {canManage && (
            <button
              type="button"
              className="btn btn--sm members__guest-add"
              onClick={() => setPrompt({ mode: 'add' })}
            >
              ＋ 새 게스트 추가
            </button>
          )}

          {guests.length === 0 ? (
            <span className="muted" style={{ fontSize: 12 }}>
              아직 등록된 게스트가 없습니다. 곡 세션 배정·일정 참석에 쓸 이름을 추가하세요.
            </span>
          ) : (
            <div className="members__guest-list">
              {guests.map((g) => (
                <div key={g.id} className="members__guest-row">
                  <Avatar
                    label={[...g.name][0] ?? '게'}
                    size={24}
                    color="var(--color-neutral-500)"
                  />
                  <span className="stack" style={{ flex: 1, minWidth: 0, gap: 2 }}>
                    <span style={{ fontSize: 13, fontWeight: 600 }}>{g.name}</span>
                    <span className="muted" style={{ fontSize: 10 }}>
                      {g.session ? `세션 · ${g.session}` : '세션 미지정'}
                    </span>
                  </span>
                  {canManage && (
                    <>
                      <button
                        type="button"
                        className="members__guest-btn"
                        onClick={() => setSessionFor(g)}
                      >
                        세션
                      </button>
                      <button
                        type="button"
                        className="members__guest-btn"
                        onClick={() => setPrompt({ mode: 'rename', guest: g })}
                      >
                        이름
                      </button>
                      <button
                        type="button"
                        className="members__guest-btn members__guest-btn--danger"
                        onClick={() => {
                          if (
                            confirm(
                              `게스트 "${g.name}" 를 삭제할까요? 세션 배정·참석에서도 빠집니다.`,
                            )
                          )
                            onRemove(g.id);
                        }}
                      >
                        삭제
                      </button>
                    </>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {prompt && (
        <PromptModal
          title={prompt.mode === 'add' ? '게스트 추가' : '게스트 이름 수정'}
          label="게스트 이름"
          placeholder="예: 세션 드러머"
          initial={prompt.mode === 'rename' ? prompt.guest.name : ''}
          submitLabel={prompt.mode === 'add' ? '추가' : '변경'}
          maxLength={50}
          onSubmit={(name) => {
            if (prompt.mode === 'add') onAdd(name);
            else onRename(prompt.guest.id, name);
            setPrompt(null);
          }}
          onClose={() => setPrompt(null)}
        />
      )}

      {sessionFor && (
        <GuestSessionModal
          name={sessionFor.name}
          current={sessionFor.session}
          onSave={(s) => {
            onSetSession(sessionFor.id, s);
            setSessionFor(null);
          }}
          onClose={() => setSessionFor(null)}
        />
      )}
    </div>
  );
}
