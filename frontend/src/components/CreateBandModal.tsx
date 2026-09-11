import { useState } from 'react';
import { useApp } from '../store/AppContext';
import type { BandVisibility } from '../types';
import { Modal } from './Modal';
import { VISIBILITY_LABEL, VISIBILITY_HINT, VISIBILITY_ORDER } from '../lib/bandVisibility';

export function CreateBandModal() {
  const { createBand, closeCreate } = useApp();
  const [name, setName] = useState('');
  const [visibility, setVisibility] = useState<BandVisibility>('PUBLIC');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canCreate = name.trim().length > 0 && !submitting;

  const submit = async () => {
    if (!canCreate) return;
    setSubmitting(true);
    setError(null);
    try {
      // createBand 가 밴드 생성 후 해당 밴드로 이동시키고 모달을 닫는다
      await createBand(name, visibility);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '밴드를 만들지 못했습니다.');
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="새 밴드 만들기"
      width={360}
      onClose={closeCreate}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            disabled={!canCreate}
            onClick={submit}
          >
            {submitting ? '만드는 중…' : '밴드 만들기'}
          </button>
          <button type="button" className="btn" onClick={closeCreate}>
            취소
          </button>
        </>
      }
    >
      <div className="field">
        <label htmlFor="band-name">밴드 이름</label>
        <input
          id="band-name"
          className="input"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="예: 목요일의 소음"
          autoFocus
        />
      </div>

      <div className="field">
        <label>공개범위</label>
        <div className="seg">
          {VISIBILITY_ORDER.map((v) => (
            <button
              key={v}
              type="button"
              className={`seg__opt${visibility === v ? ' seg__opt--on' : ''}`}
              onClick={() => setVisibility(v)}
            >
              {VISIBILITY_LABEL[v]}
            </button>
          ))}
        </div>
        <span className="muted" style={{ fontSize: 11 }}>
          {VISIBILITY_HINT[visibility]}
        </span>
      </div>

      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field">
          <label>로고</label>
          <div className="upload-box upload-box--square">업로드</div>
        </div>
        <div className="field" style={{ flex: 1 }}>
          <label>배너</label>
          <div className="upload-box">이미지 업로드</div>
        </div>
      </div>

      <p className="muted" style={{ fontSize: 11, margin: 0, lineHeight: 1.5 }}>
        만들면 내가 관리자가 됩니다. 로고·배너는 밴드 생성 후 설정할 수 있습니다.
      </p>
      {error && <p style={{ fontSize: 12, margin: 0, color: 'var(--color-accent)' }}>{error}</p>}
    </Modal>
  );
}
