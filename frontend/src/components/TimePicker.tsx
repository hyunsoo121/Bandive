import { useEffect, useRef } from 'react';
import './TimePicker.css';

interface Props {
  /** HH:mm (24시간제). */
  value: string;
  onChange: (value: string) => void;
}

const HOURS = Array.from({ length: 24 }, (_, i) => i);
const MINUTES = Array.from({ length: 60 }, (_, i) => i);

function parseTime(value: string): { h: number; m: number } {
  const [h, m] = value.split(':').map(Number);
  return { h: Number.isNaN(h) ? 0 : h, m: Number.isNaN(m) ? 0 : m };
}

function toTimeValue(h: number, m: number): string {
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** "오전/오후 h:mm" 요약 표시. */
function formatSummary(h: number, m: number): string {
  const period = h < 12 ? '오전' : '오후';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${period} ${h12}:${String(m).padStart(2, '0')}`;
}

/**
 * 시/분을 각각 스크롤 목록에서 고르는 방식 — 30분 단위가 아니라 1분 단위까지 전부 선택 가능해야
 * 해서, 시간 하나를 60줄짜리 단일 목록으로 다 늘어놓는 대신 시(24)·분(60) 두 열로 나눴다.
 * DatePicker 와 같은 이유로 토글 없이 항상 펼쳐두고, 열마다 고정 높이 안에서만 스크롤된다.
 */
export function TimePicker({ value, onChange }: Props) {
  const { h, m } = parseTime(value);
  const hourRef = useRef<HTMLButtonElement>(null);
  const minuteRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    hourRef.current?.scrollIntoView({ block: 'center' });
    minuteRef.current?.scrollIntoView({ block: 'center' });
    // 처음 열릴 때 선택된 시각으로 한 번만 스크롤.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="timepicker panel">
      <div className="timepicker__summary">{formatSummary(h, m)}</div>

      <div className="timepicker__cols">
        <div className="timepicker__col">
          {HOURS.map((hh) => {
            const isSelected = hh === h;
            return (
              <button
                key={hh}
                ref={isSelected ? hourRef : undefined}
                type="button"
                className={`timepicker__opt${isSelected ? ' is-selected' : ''}`}
                onClick={() => onChange(toTimeValue(hh, m))}
              >
                {String(hh).padStart(2, '0')}
              </button>
            );
          })}
        </div>

        <div className="timepicker__col">
          {MINUTES.map((mm) => {
            const isSelected = mm === m;
            return (
              <button
                key={mm}
                ref={isSelected ? minuteRef : undefined}
                type="button"
                className={`timepicker__opt${isSelected ? ' is-selected' : ''}`}
                onClick={() => onChange(toTimeValue(h, mm))}
              >
                {String(mm).padStart(2, '0')}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
