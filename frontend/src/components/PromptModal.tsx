import { useState } from 'react';
import { Modal } from './Modal';

interface Props {
  title: string;
  label: string;
  initial?: string;
  placeholder?: string;
  submitLabel?: string;
  maxLength?: number;
  onSubmit: (value: string) => void;
  onClose: () => void;
}

/** 한 줄 텍스트를 받는 작은 모달. window.prompt 대체. */
export function PromptModal({
  title,
  label,
  initial = '',
  placeholder,
  submitLabel = '확인',
  maxLength = 60,
  onSubmit,
  onClose,
}: Props) {
  const [value, setValue] = useState(initial);
  const trimmed = value.trim();
  const canSubmit = trimmed.length > 0 && trimmed !== initial.trim();

  const submit = () => {
    if (canSubmit) onSubmit(trimmed);
  };

  return (
    <Modal
      title={title}
      width={320}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            disabled={!canSubmit}
            onClick={submit}
          >
            {submitLabel}
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="field">
        <label htmlFor="prompt-input">{label}</label>
        <input
          id="prompt-input"
          className="input"
          value={value}
          maxLength={maxLength}
          placeholder={placeholder}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.nativeEvent.isComposing) submit();
          }}
          autoFocus
        />
      </div>
    </Modal>
  );
}
