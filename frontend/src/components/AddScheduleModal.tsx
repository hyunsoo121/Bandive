import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { KIND_LABEL } from '../lib/schedule';
import type { ScheduleEvent, ScheduleType } from '../types';
import { Modal } from './Modal';
import { DatePicker } from './DatePicker';
import { TimePicker } from './TimePicker';

interface Props {
  bandId: string;
  /** 있으면 수정 모드 — 이 일정의 값을 채우고 PATCH 로 저장 */
  editing?: ScheduleEvent;
  onClose: () => void;
  onSubmitted: (schedule: ScheduleEvent) => void;
}

const TYPES: ScheduleType[] = ['REHEARSAL', 'PERFORMANCE'];

/** ISO(UTC) → 로컬 기준 "yyyy-mm-dd"/"HH:mm" — DatePicker/TimePicker 프리필용. */
function toLocalDateInput(iso: string): string {
  const d = new Date(iso);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
function toLocalTimeInput(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

export function AddScheduleModal({ bandId, editing, onClose, onSubmitted }: Props) {
  const { addSchedule, updateSchedule } = useApp();

  const [title, setTitle] = useState(editing?.title ?? '');
  const [type, setType] = useState<ScheduleType>(editing?.type ?? 'REHEARSAL');
  const [date, setDate] = useState(editing ? toLocalDateInput(editing.dateTime) : '');
  const [time, setTime] = useState(editing ? toLocalTimeInput(editing.dateTime) : '19:00');
  const [location, setLocation] = useState(editing?.location ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = date.length > 0 && time.length > 0 && !submitting;

  const submit = async () => {
    if (!canSubmit) return;
    const parsed = new Date(`${date}T${time}`);
    if (Number.isNaN(parsed.getTime())) {
      setError('일시를 확인해 주세요.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const payload = { type, dateTime: parsed.toISOString(), location, title };
      const schedule = editing
        ? await updateSchedule(editing.id, payload)
        : await addSchedule({ bandId, ...payload });
      onSubmitted(schedule);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '일정을 저장하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={editing ? '일정 수정' : '일정 등록'}
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
            {submitting ? '저장 중…' : editing ? '수정 저장' : '일정 등록'}
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="field">
        <label htmlFor="sched-title">제목 · 선택</label>
        <input
          id="sched-title"
          className="input"
          value={title}
          maxLength={100}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={`예: ${KIND_LABEL[type]} 전 파트 리허설`}
        />
      </div>

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
        <label>날짜</label>
        <DatePicker value={date} onChange={setDate} />
      </div>

      <div className="field">
        <label>시간</label>
        <TimePicker value={time} onChange={setTime} />
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
