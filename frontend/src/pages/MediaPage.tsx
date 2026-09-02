import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { schedulesOfBand } from '../mock/selectors';
import type { MediaKind } from '../types';
import { Fab } from '../components/Fab';
import { AddMediaModal } from '../components/AddMediaModal';
import './MediaPage.css';

type Filter = '전체' | MediaKind;
const FILTERS: Filter[] = ['전체', '합주', '공연'];

const STRIPE_SHADES = [
  ['#9b9797', '#bab6b6'],
  ['#7d7979', '#9b9797'],
  ['#605d5d', '#7d7979'],
  ['#444141', '#605d5d'],
];
const stripe = (a: string, b: string) =>
  `repeating-linear-gradient(135deg, ${a} 0 12px, ${b} 12px 24px)`;

export function MediaPage() {
  const { currentBand, role, media: allMedia } = useApp();
  const guard = useGuard();
  const isGuest = role === 'guest';

  const [filter, setFilter] = useState<Filter>('전체');
  const [addOpen, setAddOpen] = useState(false);

  if (!currentBand) return null;
  const bandId = currentBand.id;

  const schedules = schedulesOfBand(bandId);
  const scheduleByDay = new Map(schedules.map((s) => [s.day, s]));

  const bandMedia = allMedia.filter((m) => m.bandId === bandId);
  // 공개범위 적용: 비회원은 '멤버만' 영상 제외 (기획서 8.7)
  const visible = bandMedia.filter((m) => !isGuest || m.visibility === '링크 공개');
  const hiddenCount = bandMedia.length - visible.length;
  const list = visible.filter((m) => filter === '전체' || m.kind === filter);

  return (
    <div className="media">
      <header className="media__head">
        <h2>영상</h2>
        <span className="media__badge">URL 첨부</span>
      </header>

      <div className="media__filters">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            className={`media__chip${filter === f ? ' is-on' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f}
          </button>
        ))}
      </div>

      {isGuest && hiddenCount > 0 && (
        <p className="media__notice">멤버 전용 영상 {hiddenCount}개는 로그인 후 볼 수 있습니다.</p>
      )}

      <div className="media__grid">
        {list.map((m, i) => {
          const ev = m.scheduleDay != null ? scheduleByDay.get(m.scheduleDay) : undefined;
          const [a, b] = STRIPE_SHADES[i % STRIPE_SHADES.length];
          const memberOnly = m.visibility === '멤버만';
          return (
            <article key={m.id} className="media__card">
              <div className="media__thumb" style={{ background: stripe(a, b) }}>
                <span className="media__play" />
                <span className="media__kind">{m.kind}</span>
              </div>
              <div className="media__card-body">
                <strong className="media__title">{m.title}</strong>
                <span className="muted" style={{ fontSize: 11 }}>
                  {m.source} · {m.date}
                </span>
                <span
                  className="media__link"
                  style={{ color: ev ? 'var(--color-accent-700)' : 'var(--color-neutral-600)' }}
                >
                  {ev ? `일정 · 8/${ev.day} ${ev.title}` : '연결된 일정 없음'}
                </span>
                <span
                  className="media__scope"
                  style={{
                    background: memberOnly ? 'var(--color-neutral-200)' : 'var(--color-accent-200)',
                    color: memberOnly ? 'var(--color-neutral-800)' : 'var(--color-accent-800)',
                  }}
                >
                  {m.visibility}
                </span>
              </div>
            </article>
          );
        })}

        {list.length === 0 && (
          <div className="media__empty">
            {bandMedia.length === 0
              ? '아직 등록된 영상이 없습니다.'
              : '이 필터에 해당하는 영상이 없습니다.'}
          </div>
        )}
      </div>

      <Fab label="＋ 영상 URL 첨부" onClick={guard(() => setAddOpen(true))} />

      {addOpen && (
        <AddMediaModal
          bandId={bandId}
          schedules={schedules}
          onClose={() => setAddOpen(false)}
          onSubmitted={() => {
            setAddOpen(false);
            setFilter('전체');
          }}
        />
      )}
    </div>
  );
}
