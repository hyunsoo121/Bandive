import { useEffect, useState } from 'react';
import './DatePicker.css';

interface Props {
  /** yyyy-mm-dd. 빈 문자열이면 미선택. */
  value: string;
  onChange: (value: string) => void;
}

const DOW = ['일', '월', '화', '수', '목', '금', '토'];

function toDateInput(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** "yyyy-mm-dd" → 로컬 자정 Date. 빈 값이면 null. */
function parseDateInput(value: string): Date | null {
  if (!value) return null;
  const [y, m, d] = value.split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

/** 이번 달의 날짜 그리드 — 일요일 시작, 앞뒤 달 날짜로 6주(42칸) 채움. */
function buildGrid(viewYear: number, viewMonth: number): Date[] {
  const first = new Date(viewYear, viewMonth, 1);
  const start = new Date(viewYear, viewMonth, 1 - first.getDay());
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
}

const sameDay = (a: Date, b: Date) =>
  a.getFullYear() === b.getFullYear() &&
  a.getMonth() === b.getMonth() &&
  a.getDate() === b.getDate();

/** 캘린더 위에 보여줄 "지금 선택된 날짜" 한 줄 — yyyy-mm-dd (요일). */
function formatSummary(d: Date | null): string {
  if (!d) return '날짜를 선택해 주세요';
  return `${toDateInput(d)} (${DOW[d.getDay()]})`;
}

/**
 * 항상 펼쳐진 캘린더 그리드 (토글 없음) — 날짜 선택 자체가 목적인 화면이라 접었다 펴는 것보다 계속
 * 보이는 게 더 편하다. 네이티브 &lt;input type="date"&gt; 는 브라우저마다 UI 가 제각각이라(맥 크롬은
 * 숫자 스피너뿐) 대체.
 */
export function DatePicker({ value, onChange }: Props) {
  const selected = parseDateInput(value);
  const [viewYear, setViewYear] = useState(() => (selected ?? new Date()).getFullYear());
  const [viewMonth, setViewMonth] = useState(() => (selected ?? new Date()).getMonth());

  // 부모가 날짜를 바꾸면 달력이 보고 있는 달도 따라간다.
  useEffect(() => {
    if (selected) {
      setViewYear(selected.getFullYear());
      setViewMonth(selected.getMonth());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const today = new Date();
  const grid = buildGrid(viewYear, viewMonth);

  const changeMonth = (delta: number) => {
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
  };

  return (
    <div className="datepicker panel">
      <div className="datepicker__summary">{formatSummary(selected)}</div>

      <div className="datepicker__nav">
        <button type="button" className="datepicker__navbtn" onClick={() => changeMonth(-1)}>
          ‹
        </button>
        <strong className="datepicker__label">
          {viewYear}년 {viewMonth + 1}월
        </strong>
        <button type="button" className="datepicker__navbtn" onClick={() => changeMonth(1)}>
          ›
        </button>
      </div>

      <div className="datepicker__dow">
        {DOW.map((d) => (
          <span key={d}>{d}</span>
        ))}
      </div>

      <div className="datepicker__grid">
        {grid.map((d) => {
          const outside = d.getMonth() !== viewMonth;
          const isToday = sameDay(d, today);
          const isSelected = selected ? sameDay(d, selected) : false;
          return (
            <button
              key={d.toISOString()}
              type="button"
              className={`datepicker__day${outside ? ' is-outside' : ''}${isToday ? ' is-today' : ''}${isSelected ? ' is-selected' : ''}`}
              onClick={() => onChange(toDateInput(d))}
            >
              {d.getDate()}
            </button>
          );
        })}
      </div>

      {(viewYear !== today.getFullYear() || viewMonth !== today.getMonth()) && (
        <button
          type="button"
          className="datepicker__today"
          onClick={() => {
            setViewYear(today.getFullYear());
            setViewMonth(today.getMonth());
          }}
        >
          오늘로 이동
        </button>
      )}
    </div>
  );
}
