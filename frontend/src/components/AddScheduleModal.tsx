import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { KIND_LABEL } from '../lib/schedule';
import type { ScheduleType } from '../types';
import { Modal } from './Modal';

interface Props {
  bandId: string;
  onClose: () => void;
  onSubmitted: () => void;
}

const TYPES: ScheduleType[] = ['REHEARSAL', 'PERFORMANCE'];

export function AddScheduleModal({ bandId, onClose, onSubmitted }: Props) {
  const { addSchedule } = useApp();

  const [type, setType] = useState<ScheduleType>('REHEARSAL');
  const [local, setLocal] = useState('');
  const [location, setLocation] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = local.length > 0 && !submitting;

  const submit = async () => {
    if (!canSubmit) return;
    const parsed = new Date(local);
    if (Number.isNaN(parsed.getTime())) {
      setError('일시를 확인해 주세요.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await addSchedule({
        bandId,
        type,
        dateTime: parsed.toISOString(),
        location,
      });
      onSubmitted();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '일정을 등록하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="일정 등록"
      width={380}
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
            {submitting ? '등록 중…' : '일정 등록'}
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="field">
        <label>종류</label>
        <div className="seg">
          {TYPES.map((t) => (
            <button
              key={t}
              type="button"
              className={`seg__opt${type === t ? ' seg__opt--on' : ''}`}
              onClick={() => setType(t)}
            >
              {KIND_LABEL[t]}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <label htmlFor="sched-when">일시</label>
        <input
          id="sched-when"
          type="datetime-local"
          className="input"
          value={local}
          onChange={(e) => setLocal(e.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="sched-place">장소 · 선택</label>
        <input
          id="sched-place"
          className="input"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          placeholder="예: 홍대 사운드 스튜디오 B"
        />
      </div>

      {error && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{error}</span>}
    </Modal>
  );
}
