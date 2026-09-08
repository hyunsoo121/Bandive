import { useState } from 'react';
import type { Guest } from '../types';
import { INSTRUMENTS } from '../types';
import { Modal } from './Modal';
import './GuestPickerModal.css';

const SESSION_OPTIONS = [...INSTRUMENTS, '관객'];

interface Props {
  /** 밴드에 등록된 게스트 전체 */
  guests: Guest[];
  /** 이미 이 일정에 들어가 있는 게스트 id — 목록에서 뺀다 */
  addedGuestIds: string[];
  /** 새 게스트 등록 (관리자). 같은 이름이면 reject */
  onAddNew: (name: string) => Promise<Guest>;
  /** 선택한 게스트를 일정에 추가 */
  onConfirm: (guestId: string, session: string | null) => Promise<void>;
  onClose: () => void;
}

export function GuestPickerModal({ guests, addedGuestIds, onAddNew, onConfirm, onClose }: Props) {
  const available = guests.filter((g) => !addedGuestIds.includes(g.id));

  const [guestId, setGuestId] = useState<string>('');
  const [session, setSession] = useState<string>('');
  const [customSession, setCustomSession] = useState('');
  const [newName, setNewName] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const effectiveSession = (session || customSession.trim()).slice(0, 30) || null;

  const addNew = async () => {
    const name = newName.trim();
    if (!name || busy) return;
    setBusy(true);
    setErr(null);
    try {
      const created = await onAddNew(name);
      setNewName('');
      setGuestId(created.id);
    } catch (e) {
      setErr(e instanceof Error ? e.message : '게스트를 추가하지 못했습니다.');
    } finally {
      setBusy(false);
    }
  };

  const confirm = async () => {
    if (!guestId || busy) return;
    setBusy(true);
    setErr(null);
    try {
      await onConfirm(guestId, effectiveSession);
      onClose();
    } catch (e) {
      setErr(e instanceof Error ? e.message : '일정에 추가하지 못했습니다.');
      setBusy(false);
    }
  };

  return (
    <Modal
      title="게스트 추가"
      width={380}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            disabled={!guestId || busy}
            onClick={confirm}
          >
            {busy ? '처리 중…' : '이 일정에 추가'}
          </button>
          <button type="button" className="btn" onClick={onClose} disabled={busy}>
            취소
          </button>
        </>
      }
    >
      <div className="stack" style={{ gap: 14 }}>
        <div className="stack" style={{ gap: 8 }}>
          <span className="guestpick__label">등록된 게스트</span>
          {available.length === 0 ? (
            <span className="muted" style={{ fontSize: 12 }}>
              선택할 수 있는 게스트가 없습니다. 아래에서 새로 추가하세요.
            </span>
          ) : (
            <div className="guestpick__list">
              {available.map((g) => (
                <button
                  key={g.id}
                  type="button"
                  className={`guestpick__opt${guestId === g.id ? ' is-on' : ''}`}
                  onClick={() => setGuestId(g.id)}
                >
                  {g.name}
                </button>
              ))}
            </div>
          )}
          <div className="guestpick__add">
            <input
              className="input"
              value={newName}
              maxLength={50}
              placeholder="새 게스트 이름"
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  void addNew();
                }
              }}
            />
            <button
              type="button"
              className="btn btn--sm"
              onClick={addNew}
              disabled={!newName.trim() || busy}
            >
              추가
            </button>
          </div>
        </div>

        <div className="stack" style={{ gap: 8 }}>
          <span className="guestpick__label">세션 (선택)</span>
          <div className="guestpick__sessions">
            {SESSION_OPTIONS.map((s) => (
              <button
                key={s}
                type="button"
                className={`guestpick__chip${session === s ? ' is-on' : ''}`}
                onClick={() => {
                  setSession((cur) => (cur === s ? '' : s));
                  setCustomSession('');
                }}
              >
                {s}
              </button>
            ))}
          </div>
          <input
            className="input"
            value={customSession}
            maxLength={30}
            placeholder="직접 입력 (예: 신디, 코러스)"
            onChange={(e) => {
              setCustomSession(e.target.value);
              if (e.target.value) setSession('');
            }}
          />
          {session === '' && customSession.trim() === '' && (
            <span className="muted" style={{ fontSize: 11 }}>
              비워두면 세션 미지정으로 추가됩니다.
            </span>
          )}
        </div>

        {err && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{err}</span>}
      </div>
    </Modal>
  );
}
