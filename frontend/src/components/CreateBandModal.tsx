import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { Modal } from './Modal';

interface Props {
  onCreated: (bandId: string) => void;
}

export function CreateBandModal({ onCreated }: Props) {
  const { bands, createBand, closeCreate } = useApp();
  const [name, setName] = useState('');
  const canCreate = name.trim().length > 0;

  const submit = () => {
    if (!canCreate) return;
    createBand(name);
    // createBand 가 새 id 를 n{len+1} 규칙으로 만든다
    onCreated(`n${bands.length + 1}`);
  };

  return (
    <Modal
      title="새 밴드 만들기"
      width={360}
      onClose={closeCreate}
      footer={
        <>
          <button type="button" className="btn btn--primary" style={{ flex: 1 }} disabled={!canCreate} onClick={submit}>
            밴드 만들기
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
        만들면 내가 밴드장이 되고, 초대 코드가 바로 발급됩니다.
      </p>
    </Modal>
  );
}
