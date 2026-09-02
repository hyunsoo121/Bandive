import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { KIND_LABEL, schedulesOfBand } from '../mock/selectors';
import type { AttendanceStatus, ScheduleEvent } from '../types';
import { Avatar } from '../components/Avatar';
import { Fab } from '../components/Fab';
import './SchedulePage.css';

const DOW = ['일', '월', '화', '수', '목', '금', '토'];
const ATT_OPTIONS: AttendanceStatus[] = ['참석', '미정', '불참'];

/** 목업 시드 데이터가 있는 달 (2026년 8월, month 는 0-indexed) */
const DATA_YEAR = 2026;
const DATA_MONTH = 7;
const TODAY_DAY = 28;

/** 멤버별 기본 출결 (목업 하드코딩값) — '내' 응답은 UI 선택으로 덮어씀 */
const BASE_ATTENDANCE: Record<string, AttendanceStatus> = {
  김현수: '참석',
  전환: '참석',
  송민호: '참석',
  김시훈: '미정',
  윤수빈: '불참',
};

function statusStyle(s: AttendanceStatus): { background: string; color: string } {
  if (s === '참석')
    return { background: 'var(--color-accent-200)', color: 'var(--color-accent-800)' };
  if (s === '불참')
    return { background: 'var(--color-neutral-200)', color: 'var(--color-neutral-700)' };
  return { background: 'transparent', color: 'var(--color-neutral-700)' };
}

interface Cell {
  day: number | null;
  event: ScheduleEvent | null;
}

function buildCells(year: number, month: number, events: ScheduleEvent[]): Cell[] {
  const startDow = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const byDay = new Map(events.map((e) => [e.day, e]));
  return Array.from({ length: 42 }, (_, i) => {
    const day = i - startDow + 1;
    const inMonth = day >= 1 && day <= daysInMonth;
    return { day: inMonth ? day : null, event: inMonth ? (byDay.get(day) ?? null) : null };
  });
}

export function SchedulePage() {
  const { currentBand, user, media: allMedia, members: allMembers } = useApp();
  const guard = useGuard();
  const bandId = currentBand?.id ?? '';

  const events = schedulesOfBand(bandId);
  const members = allMembers.filter((m) => m.bandId === bandId);
  const media = allMedia.filter((m) => m.bandId === bandId);

  const [view, setView] = useState({ year: DATA_YEAR, month: DATA_MONTH });
  const isDataMonth = view.year === DATA_YEAR && view.month === DATA_MONTH;
  const monthEvents = isDataMonth ? events : [];

  const firstUpcoming =
    events.find((e) => e.day >= TODAY_DAY)?.day ?? events[events.length - 1]?.day ?? null;
  const [selectedDay, setSelectedDay] = useState<number | null>(firstUpcoming);
  const [myAtt, setMyAtt] = useState<Record<string, AttendanceStatus>>({});

  if (!currentBand) return null;

  const cells = buildCells(view.year, view.month, monthEvents);
  const selected = isDataMonth ? (monthEvents.find((e) => e.day === selectedDay) ?? null) : null;
  const selectedMedia = selected ? media.filter((m) => m.scheduleDay === selected.day) : [];

  const shiftMonth = (delta: number) => {
    setView((v) => {
      const d = new Date(v.year, v.month + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() };
    });
  };

  const myName = user?.name ?? '';
  const attendanceRows = members.map((m) => {
    const status =
      m.name === myName
        ? (myAtt[selected?.id ?? ''] ?? BASE_ATTENDANCE[m.name] ?? '미정')
        : (BASE_ATTENDANCE[m.name] ?? '미정');
    return { ...m, status };
  });
  const goingCount = attendanceRows.filter((r) => r.status === '참석').length;
  const myStatus = selected ? (myAtt[selected.id] ?? BASE_ATTENDANCE[myName]) : undefined;

  return (
    <div className="sched">
      <header className="sched__head">
        <h2>일정</h2>
        <div className="sched__monthnav">
          <button type="button" onClick={() => shiftMonth(-1)} aria-label="이전 달">
            ‹
          </button>
          <span>
            {view.year}. {String(view.month + 1).padStart(2, '0')}
          </span>
          <button type="button" onClick={() => shiftMonth(1)} aria-label="다음 달">
            ›
          </button>
        </div>
      </header>

      {/* 캘린더 */}
      <div className="sched__dow">
        {DOW.map((d, i) => (
          <span
            key={d}
            style={{ color: i === 0 ? 'var(--color-accent-700)' : 'var(--color-neutral-700)' }}
          >
            {d}
          </span>
        ))}
      </div>
      <div className="sched__grid">
        {cells.map((c, i) => {
          const isSel = c.day != null && c.day === selectedDay && isDataMonth;
          return (
            <button
              key={i}
              type="button"
              className={`sched__cell${isSel ? ' is-sel' : ''}`}
              disabled={c.day == null}
              onClick={() => c.day != null && setSelectedDay(c.day)}
            >
              <span
                className="sched__cell-n"
                style={{
                  fontWeight: isSel ? 800 : c.event ? 700 : 500,
                  color: i % 7 === 0 ? 'var(--color-accent-700)' : 'var(--color-text)',
                }}
              >
                {c.day ?? ''}
              </span>
              <span
                className="sched__dot"
                style={{
                  background: c.event
                    ? c.event.type === 'PERFORMANCE'
                      ? 'var(--color-neutral-800)'
                      : 'var(--color-accent)'
                    : 'transparent',
                }}
              />
              <span className="sched__cell-tag">{c.event ? KIND_LABEL[c.event.type] : ''}</span>
            </button>
          );
        })}
      </div>

      <div className="sched__legend">
        <span>
          <i style={{ background: 'var(--color-accent)' }} /> 연습
        </span>
        <span>
          <i style={{ background: 'var(--color-neutral-800)' }} /> 공연
        </span>
      </div>

      {/* 리스트 + 상세 */}
      <div className="sched__cols">
        <section className="sched__list-col">
          <span className="kicker">{view.month + 1}월 일정</span>
          <div className="sched__list">
            {monthEvents.length === 0 && (
              <div className="sched__empty">이 달에는 등록된 일정이 없습니다.</div>
            )}
            {monthEvents.map((e) => (
              <button
                key={e.id}
                type="button"
                className={`sched__event${e.day === selectedDay ? ' is-sel' : ''}`}
                onClick={() => setSelectedDay(e.day)}
              >
                <span className="sched__event-date">
                  <strong>{e.day}</strong>
                  <span className="muted">{e.dow}</span>
                </span>
                <span className="stack" style={{ gap: 4, flex: 1, minWidth: 0 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span
                      className="sched__kind"
                      style={{
                        background:
                          e.type === 'PERFORMANCE'
                            ? 'var(--color-neutral-800)'
                            : 'var(--color-accent)',
                      }}
                    >
                      {KIND_LABEL[e.type]}
                    </span>
                    <span className="muted" style={{ fontSize: 11 }}>
                      {e.time}
                    </span>
                  </span>
                  <strong style={{ fontSize: 14 }}>{e.title}</strong>
                  <span className="muted" style={{ fontSize: 11 }}>
                    {e.place}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="sched__detail-col">
          <span className="kicker">일정 상세 · 출결</span>
          {selected ? (
            <div className="panel">
              <div className="sched__detail-head">
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span
                    className="sched__kind"
                    style={{
                      background:
                        selected.type === 'PERFORMANCE'
                          ? 'var(--color-neutral-800)'
                          : 'var(--color-accent)',
                    }}
                  >
                    {KIND_LABEL[selected.type]}
                  </span>
                  <span className="muted" style={{ fontSize: 11 }}>
                    {view.month + 1}월 {selected.day}일 {selected.dow} · {selected.time}
                  </span>
                </span>
                <strong style={{ fontFamily: 'var(--font-heading)', fontSize: 18 }}>
                  {selected.title}
                </strong>
                <span className="muted" style={{ fontSize: 12 }}>
                  {selected.place}
                </span>
              </div>

              <div className="sched__section">
                <span className="sched__section-label">내 참석 여부</span>
                <div style={{ display: 'flex', gap: 8 }}>
                  {ATT_OPTIONS.map((opt) => (
                    <button
                      key={opt}
                      type="button"
                      className={`sched__att${myStatus === opt ? ' is-on' : ''}`}
                      onClick={guard(() => setMyAtt((prev) => ({ ...prev, [selected.id]: opt })))}
                    >
                      {opt}
                    </button>
                  ))}
                </div>
              </div>

              <div className="sched__section">
                <span className="sched__section-label">이 일정의 영상</span>
                {selectedMedia.length > 0 ? (
                  <div className="stack" style={{ gap: 8 }}>
                    {selectedMedia.map((m) => (
                      <div key={m.id} className="sched__vid">
                        <span className="sched__vid-thumb">
                          <span className="sched__vid-play" />
                        </span>
                        <span className="stack" style={{ minWidth: 0, flex: 1 }}>
                          <strong style={{ fontSize: 12 }}>{m.title}</strong>
                          <span className="muted" style={{ fontSize: 10 }}>
                            {m.source} · {m.date} · {m.visibility}
                          </span>
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <span className="muted" style={{ fontSize: 11 }}>
                    아직 연결된 영상이 없습니다. 영상 탭에서 이 일정에 연결할 수 있습니다.
                  </span>
                )}
              </div>

              <div className="sched__section">
                <div className="spread">
                  <span className="sched__section-label">멤버 출결</span>
                  <span style={{ fontSize: 11, fontWeight: 700 }}>
                    참석 {goingCount} / {members.length}
                  </span>
                </div>
                {attendanceRows.map((r) => {
                  const st = statusStyle(r.status);
                  return (
                    <div key={r.id} className="sched__att-row">
                      <Avatar label={r.initial} size={26} color={r.avatarColor} />
                      <span style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>{r.name}</span>
                      <span className="muted" style={{ fontSize: 11 }}>
                        {r.part}
                      </span>
                      <span className="sched__status" style={st}>
                        {r.status}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="panel sched__empty">
              {isDataMonth
                ? '일정을 선택하면 출결·영상이 표시됩니다.'
                : '이 달에는 일정이 없습니다.'}
            </div>
          )}
        </section>
      </div>

      <Fab label="＋ 일정 등록" onClick={guard(() => {})} />
    </div>
  );
}
