import { useState } from 'react';
import { INSTRUMENTS } from '../types';
import { Modal } from './Modal';
import './GuestSessionModal.css';

const OPTIONS = [...INSTRUMENTS, '관객'];

interface Props {
  name: string;
  current: string | null;
  /** 저장. 빈 값이면 세션 미지정으로 지운다. */
  onSave: (session: string | null) => void;
  onClose: () => void;
}

export function GuestSessionModal({ name, current, onSave, onClose }: Props) {
  const [sel, setSel] = useState(current ?? '');
  const [custom, setCustom] = useState(current && !OPTIONS.includes(current) ? current : '');

  const effective = (sel || custom.trim()).slice(0, 30);

  return (
    <Modal
      title={`세션 설정 · ${name}`}
      width={360}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            onClick={() => onSave(effective || null)}
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
          하나만 고르거나 직접 입력하세요. 비우면 미지정.
        </span>
        <div className="gsess__chips">
          {OPTIONS.map((o) => (
            <button
              key={o}
              type="button"
              className={`gsess__chip${sel === o ? ' is-on' : ''}`}
              onClick={() => {
                setSel((cur) => (cur === o ? '' : o));
                setCustom('');
              }}
            >
              {o}
            </button>
          ))}
        </div>
        <input
          className="input"
          value={custom}
          maxLength={30}
          placeholder="직접 입력 (예: 신디, 코러스)"
          onChange={(e) => {
            setCustom(e.target.value);
            if (e.target.value) setSel('');
          }}
        />
      </div>
    </Modal>
  );
}
