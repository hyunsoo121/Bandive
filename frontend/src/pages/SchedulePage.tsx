import { useEffect, useMemo, useState } from 'react';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { KIND_LABEL, byDateAsc, toUi, type UiSchedule } from '../lib/schedule';
import type { AttendanceStatus } from '../types';
import { Avatar } from '../components/Avatar';
import { Fab } from '../components/Fab';
import { AddScheduleModal } from '../components/AddScheduleModal';
import './SchedulePage.css';

const ATT_OPTIONS: AttendanceStatus[] = ['참석', '미정', '불참'];

function statusStyle(s: AttendanceStatus): { background: string; color: string } {
  if (s === '참석')
    return { background: 'var(--color-accent-200)', color: 'var(--color-accent-800)' };
  if (s === '불참')
    return { background: 'var(--color-neutral-200)', color: 'var(--color-neutral-700)' };
  return { background: 'transparent', color: 'var(--color-neutral-700)' };
}

interface Cell {
  day: number | null;
  events: UiSchedule[];
}

function buildCells(year: number, month: number, events: UiSchedule[]): Cell[] {
  const startDow = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  return Array.from({ length: 42 }, (_, i) => {
    const day = i - startDow + 1;
    const inMonth = day >= 1 && day <= daysInMonth;
    return {
      day: inMonth ? day : null,
      events: inMonth ? events.filter((e) => e.day === day) : [],
    };
  });
}

const DOW = ['일', '월', '화', '수', '목', '금', '토'];

export function SchedulePage() {
  const { currentBand, media: allMedia, members: allMembers, schedules, setAttendance } = useApp();
  const guard = useGuard();
  const bandId = currentBand?.id ?? '';

  const uiEvents = useMemo(() => [...schedules].sort(byDateAsc).map(toUi), [schedules]);
  const members = allMembers.filter((m) => m.bandId === bandId);
  const media = allMedia.filter((m) => m.bandId === bandId);

  const [view, setView] = useState(() => {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() };
  });
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [pending, setPending] = useState(false);

  // 밴드가 바뀌면 이전 밴드의 선택을 버린다 → 아래 초점 effect 가 새 밴드 기준으로 다시 돈다.
  useEffect(() => {
    setSelectedId(null);
  }, [bandId]);

  // 데이터가 들어오면 가장 가까운(또는 마지막) 일정으로 초점을 맞춘다.
  useEffect(() => {
    if (selectedId || uiEvents.length === 0) return;
    const now = new Date();
    const focus = uiEvents.find((e) => e.at >= now) ?? uiEvents[uiEvents.length - 1];
    setSelectedId(focus.id);
    setView({ year: focus.year, month: focus.month });
  }, [uiEvents, selectedId]);

  if (!currentBand) return null;

  const monthEvents = uiEvents.filter((e) => e.year === view.year && e.month === view.month);
  const cells = buildCells(view.year, view.month, monthEvents);
  const selected = uiEvents.find((e) => e.id === selectedId) ?? null;
  const selectedMedia = selected ? media.filter((m) => selected.mediaIds.includes(m.id)) : [];

  const shiftMonth = (delta: number) => {
    setView((v) => {
      const d = new Date(v.year, v.month + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() };
    });
  };

  const attendanceRows = members.map((m) => {
    const found = selected?.attendees.find((a) => a.userId === m.id);
    return { ...m, status: found?.status ?? ('미정' as AttendanceStatus) };
  });
  const goingCount = selected?.counts.attending ?? 0;
  const myStatus = selected?.myStatus ?? undefined;

  const chooseAttendance = async (opt: AttendanceStatus) => {
    if (!selected) return;
    setPending(true);
    try {
      await setAttendance(selected.id, opt);
    } finally {
      setPending(false);
    }
  };

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
          const ev = c.events[0] ?? null;
          const isSel = ev != null && ev.id === selectedId;
          return (
            <button
              key={i}
              type="button"
              className={`sched__cell${isSel ? ' is-sel' : ''}`}
              disabled={c.day == null}
              onClick={() => ev && setSelectedId(ev.id)}
            >
              <span
                className="sched__cell-n"
                style={{
                  fontWeight: isSel ? 800 : ev ? 700 : 500,
                  color: i % 7 === 0 ? 'var(--color-accent-700)' : 'var(--color-text)',
                }}
              >
                {c.day ?? ''}
              </span>
              <span
                className="sched__dot"
                style={{
                  background: ev
                    ? ev.type === 'PERFORMANCE'
                      ? 'var(--color-neutral-800)'
                      : 'var(--color-accent)'
                    : 'transparent',
                }}
              />
              <span className="sched__cell-tag">
                {ev ? KIND_LABEL[ev.type] : ''}
                {c.events.length > 1 ? ` +${c.events.length - 1}` : ''}
              </span>
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
                className={`sched__event${e.id === selectedId ? ' is-sel' : ''}`}
                onClick={() => setSelectedId(e.id)}
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
                      {e.timeLabel}
                    </span>
                  </span>
                  <strong style={{ fontSize: 14 }}>{e.location || KIND_LABEL[e.type]}</strong>
                  <span className="muted" style={{ fontSize: 11 }}>
                    {e.location || '장소 미정'}
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
                    {selected.month + 1}월 {selected.day}일 {selected.dow} · {selected.timeLabel}
                  </span>
                </span>
                <strong style={{ fontFamily: 'var(--font-heading)', fontSize: 18 }}>
                  {selected.location || KIND_LABEL[selected.type]}
                </strong>
                <span className="muted" style={{ fontSize: 12 }}>
                  {selected.location || '장소 미정'}
                </span>
              </div>

              <div className="sched__section">
                <span className="sched__section-label">내 참석 여부</span>
                <div style={{ display: 'flex', gap: 8 }}>
                  {ATT_OPTIONS.map((opt) => (
                    <button
                      key={opt}
                      type="button"
                      disabled={pending}
                      className={`sched__att${myStatus === opt ? ' is-on' : ''}`}
                      onClick={guard(() => {
                        void chooseAttendance(opt);
                      })}
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
                      <a
                        key={m.id}
                        className="sched__vid"
                        href={m.url}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <span className="sched__vid-thumb">
                          <span className="sched__vid-play" />
                        </span>
                        <span className="stack" style={{ minWidth: 0, flex: 1 }}>
                          <strong style={{ fontSize: 12, wordBreak: 'break-all' }}>
                            {m.title}
                          </strong>
                          <span className="muted" style={{ fontSize: 10 }}>
                            {m.source} · {m.date} · {m.visibility}
                          </span>
                        </span>
                      </a>
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
              {uiEvents.length === 0
                ? '아직 등록된 일정이 없습니다.'
                : '일정을 선택하면 출결·영상이 표시됩니다.'}
            </div>
          )}
        </section>
      </div>

      <Fab label="＋ 일정 등록" onClick={guard(() => setAddOpen(true))} />

      {addOpen && (
        <AddScheduleModal
          bandId={bandId}
          onClose={() => setAddOpen(false)}
          onSubmitted={() => setAddOpen(false)}
        />
      )}
    </div>
  );
}
