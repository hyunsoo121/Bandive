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

/** yyyy-mm-dd (로컬 기준) — <input type="date"> 값과 그대로 맞는 포맷. */
function toDateInput(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

const QUICK_DAYS: { label: string; offset: number }[] = [
  { label: '오늘', offset: 0 },
  { label: '내일', offset: 1 },
  { label: '이번 주말', offset: -1 }, // 아래서 실제 오프셋 계산
  { label: '다음 주', offset: 7 },
];

/** 이번 주말(토) 까지 남은 일수. 이미 주말이면 다음 주말. */
function daysUntilWeekend(from: Date): number {
  const day = from.getDay(); // 0=일 ... 6=토
  const untilSat = (6 - day + 7) % 7;
  return untilSat === 0 ? 7 : untilSat; // 오늘이 토요일이면 다음 주 토요일
}

export function AddScheduleModal({ bandId, onClose, onSubmitted }: Props) {
  const { addSchedule } = useApp();

  const [type, setType] = useState<ScheduleType>('REHEARSAL');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('19:00');
  const [location, setLocation] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = date.length > 0 && time.length > 0 && !submitting;

  const pickQuickDay = (offset: number) => {
    const base = new Date();
    const days = offset === -1 ? daysUntilWeekend(base) : offset;
    base.setDate(base.getDate() + days);
    setDate(toDateInput(base));
  };

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
        <label htmlFor="sched-date">날짜</label>
        <div className="seg">
          {QUICK_DAYS.map((q) => (
            <button
              key={q.label}
              type="button"
              className="seg__opt"
              onClick={() => pickQuickDay(q.offset)}
            >
              {q.label}
            </button>
          ))}
        </div>
        <input
          id="sched-date"
          type="date"
          className="input"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="sched-time">시간</label>
        <input
          id="sched-time"
          type="time"
          className="input"
          value={time}
          onChange={(e) => setTime(e.target.value)}
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
