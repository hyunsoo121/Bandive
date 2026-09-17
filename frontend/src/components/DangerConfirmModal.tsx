import { useState, type ReactNode } from 'react';
import { Modal } from './Modal';
import './DangerConfirmModal.css';

interface Props {
  title: string;
  message: ReactNode;
  /** 사용자가 그대로 입력해야 확인 버튼이 활성화되는 문구 */
  confirmPhrase: string;
  confirmLabel?: string;
  /** 확인 시 실행. reject 하면 모달은 열린 채 에러를 보여준다. resolve 하면 부모가 모달을 닫는다. */
  onConfirm: () => Promise<void> | void;
  onClose: () => void;
}

export function DangerConfirmModal({
  title,
  message,
  confirmPhrase,
  confirmLabel,
  onConfirm,
  onClose,
}: Props) {
  const [typed, setTyped] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const matches = typed.trim() === confirmPhrase;

  const run = async () => {
    if (!matches || busy) return;
    setBusy(true);
    setErr(null);
    try {
      await onConfirm();
    } catch (e) {
      setErr(e instanceof Error ? e.message : '처리하지 못했습니다.');
      setBusy(false);
    }
  };

  return (
    <Modal
      title={title}
      width={380}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn dangerconfirm__go"
            style={{ flex: 1 }}
            disabled={!matches || busy}
            onClick={run}
          >
            {busy ? '처리 중…' : (confirmLabel ?? confirmPhrase)}
          </button>
          <button type="button" className="btn" onClick={onClose} disabled={busy}>
            취소
          </button>
        </>
      }
    >
      <div className="stack" style={{ gap: 12 }}>
        <p style={{ fontSize: 13, lineHeight: 1.6, margin: 0 }}>{message}</p>
        <label className="stack" style={{ gap: 6 }}>
          <span className="muted" style={{ fontSize: 12 }}>
            계속하려면 <strong style={{ color: 'var(--color-text)' }}>{confirmPhrase}</strong> 를
            입력하세요.
          </span>
          <input
            className="input"
            value={typed}
            autoFocus
            placeholder={confirmPhrase}
            onChange={(e) => setTyped(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
                e.preventDefault();
                void run();
              }
            }}
          />
        </label>
        {err && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{err}</span>}
      </div>
    </Modal>
  );
}
