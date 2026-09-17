import { useState } from 'react';
import { INSTRUMENTS } from '../types';
import { Modal } from './Modal';
import './PartsPickerModal.css';

const MAX_PARTS = 5;
const isKnown = (p: string) => INSTRUMENTS.includes(p as (typeof INSTRUMENTS)[number]);

interface Props {
  name: string;
  current: string[];
  onSave: (parts: string[]) => void;
  onClose: () => void;
}

export function PartsPickerModal({ name, current, onSave, onClose }: Props) {
  const [sel, setSel] = useState<string[]>(current);
  const [newPart, setNewPart] = useState('');
  const full = sel.length >= MAX_PARTS;
  const custom = sel.filter((p) => !isKnown(p));

  const toggle = (inst: string) =>
    setSel((s) =>
      s.includes(inst) ? s.filter((x) => x !== inst) : s.length < MAX_PARTS ? [...s, inst] : s,
    );

  const addCustom = () => {
    const v = newPart.trim().slice(0, 20);
    if (!v || full || sel.includes(v)) return;
    setSel([...sel, v]);
    setNewPart('');
  };

  return (
    <Modal
      title={`세션 설정 · ${name}`}
      width={380}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            onClick={() => onSave(sel)}
          >
            저장
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="stack" style={{ gap: 12 }}>
        <span className="muted" style={{ fontSize: 12 }}>
          여러 개 고를 수 있어요. 최대 {MAX_PARTS}개 · {sel.length}개 선택
        </span>

        <div className="partpick__grid">
          {INSTRUMENTS.map((inst) => {
            const on = sel.includes(inst);
            return (
              <button
                key={inst}
                type="button"
                className={`partpick__chip${on ? ' is-on' : ''}`}
                disabled={!on && full}
                onClick={() => toggle(inst)}
              >
                {inst}
              </button>
            );
          })}
          {custom.map((p) => (
            <button
              key={p}
              type="button"
              className="partpick__chip is-on partpick__chip--custom"
              onClick={() => setSel(sel.filter((x) => x !== p))}
              title="눌러서 제거"
            >
              {p} ✕
            </button>
          ))}
        </div>

        {full ? (
          <span className="muted" style={{ fontSize: 11 }}>
            최대 {MAX_PARTS}개까지 선택했습니다. 빼려면 위 칩을 누르세요.
          </span>
        ) : (
          <div className="partpick__add">
            <input
              className="input"
              value={newPart}
              maxLength={20}
              placeholder="직접 입력 (예: 신디, 색소폰)"
              onChange={(e) => setNewPart(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
                  e.preventDefault();
                  addCustom();
                }
              }}
            />
            <button
              type="button"
              className="btn btn--sm"
              onClick={addCustom}
              disabled={!newPart.trim()}
            >
              추가
            </button>
          </div>
        )}
      </div>
    </Modal>
  );
}
