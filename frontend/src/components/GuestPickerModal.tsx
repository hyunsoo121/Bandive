import { useState } from 'react';
import type { Guest } from '../types';
import { Modal } from './Modal';
import './GuestPickerModal.css';

interface Props {
  /** 밴드에 등록된 게스트 전체 */
  guests: Guest[];
  /** 이미 이 일정에 들어가 있는 게스트 id — 목록에서 뺀다 */
  addedGuestIds: string[];
  /** 새 게스트 등록 (관리자). 같은 이름이면 reject */
  onAddNew: (name: string) => Promise<Guest>;
  /** 선택(또는 새로 만든) 게스트를 일정에 추가 */
  onPick: (guestId: string) => Promise<void>;
  onClose: () => void;
}

export function GuestPickerModal({ guests, addedGuestIds, onAddNew, onPick, onClose }: Props) {
  const available = guests.filter((g) => !addedGuestIds.includes(g.id));

  const [newName, setNewName] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const addNew = async () => {
    const name = newName.trim();
    if (!name || busy) return;
    setBusy(true);
    setErr(null);
    try {
      const created = await onAddNew(name);
      await onPick(created.id);
      onClose();
    } catch (e) {
      setErr(e instanceof Error ? e.message : '게스트를 추가하지 못했습니다.');
      setBusy(false);
    }
  };

  const pick = async (guestId: string) => {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      await onPick(guestId);
      onClose();
    } catch (e) {
      setErr(e instanceof Error ? e.message : '일정에 추가하지 못했습니다.');
      setBusy(false);
    }
  };

  return (
    <Modal
      title="게스트 추가"
      width={360}
      onClose={onClose}
      footer={
        <button type="button" className="btn" style={{ flex: 1 }} onClick={onClose} disabled={busy}>
          닫기
        </button>
      }
    >
      <div className="stack" style={{ gap: 14 }}>
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
            className="btn btn--sm btn--primary"
            onClick={addNew}
            disabled={!newName.trim() || busy}
          >
            추가
          </button>
        </div>

        <div className="stack" style={{ gap: 6 }}>
          <span className="guestpick__label">등록된 게스트</span>
          {available.length === 0 ? (
            <span className="muted" style={{ fontSize: 12 }}>
              추가할 수 있는 게스트가 없습니다. 위에서 새로 만드세요.
            </span>
          ) : (
            <div className="guestpick__list">
              {available.map((g) => (
                <button
                  key={g.id}
                  type="button"
                  className="guestpick__row"
                  disabled={busy}
                  onClick={() => pick(g.id)}
                >
                  <span className="guestpick__row-name">{g.name}</span>
                  {g.session && <span className="guestpick__row-session">{g.session}</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        {err && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{err}</span>}
      </div>
    </Modal>
  );
}
