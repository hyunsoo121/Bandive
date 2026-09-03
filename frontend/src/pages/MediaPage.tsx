import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { KIND_LABEL, toUi } from '../lib/schedule';
import type { MediaItem, MediaKind } from '../types';
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

const PLATFORM_ICON: Record<MediaItem['platform'], string> = {
  youtube: '▶',
  drive: '△',
  other: '🔗',
};

export function MediaPage() {
  const { currentBand, role, user, media: allMedia, schedules, removeMedia } = useApp();
  const guard = useGuard();
  const isGuest = role === 'guest';

  const [filter, setFilter] = useState<Filter>('전체');
  const [addOpen, setAddOpen] = useState(false);
  const [editing, setEditing] = useState<MediaItem | null>(null);

  if (!currentBand) return null;
  const bandId = currentBand.id;

  const scheduleById = new Map(schedules.map((s) => [s.id, s]));

  const bandMedia = allMedia.filter((m) => m.bandId === bandId);
  // 공개범위 적용: 비회원은 '멤버만' 영상 제외 (기획서 8.7)
  const visible = bandMedia.filter((m) => !isGuest || m.visibility === '링크 공개');
  const hiddenCount = bandMedia.length - visible.length;
  const list = visible.filter((m) => filter === '전체' || m.kind === filter);

  const canManage = (m: MediaItem) =>
    role === 'owner' || (user != null && m.uploadedByUserId === user.id);

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
          const ev = m.scheduleId ? scheduleById.get(m.scheduleId) : undefined;
          const evUi = ev ? toUi(ev) : null;
          const [a, b] = STRIPE_SHADES[i % STRIPE_SHADES.length];
          const memberOnly = m.visibility === '멤버만';
          return (
            <article key={m.id} className="media__card">
              <a
                className="media__thumb"
                href={m.url}
                target="_blank"
                rel="noreferrer"
                style={m.thumbnailUrl ? undefined : { background: stripe(a, b) }}
              >
                {m.thumbnailUrl && (
                  <img className="media__thumb-img" src={m.thumbnailUrl} alt="" loading="lazy" />
                )}
                <span className="media__play">{PLATFORM_ICON[m.platform]}</span>
                <span className="media__kind">{m.kind}</span>
                {m.platform === 'other' && <span className="media__warn">링크 아님</span>}
              </a>
              <div className="media__card-body">
                <a
                  className="media__title"
                  href={m.url}
                  target="_blank"
                  rel="noreferrer"
                  style={{ wordBreak: 'break-all' }}
                >
                  {m.title}
                </a>
                <span className="muted" style={{ fontSize: 11 }}>
                  {m.source} · {m.date}
                </span>
                <span
                  className="media__link"
                  style={{ color: evUi ? 'var(--color-accent-700)' : 'var(--color-neutral-600)' }}
                >
                  {evUi
                    ? `일정 · ${evUi.month + 1}/${evUi.day} ${KIND_LABEL[evUi.type]}`
                    : '연결된 일정 없음'}
                </span>
                <div className="media__card-foot">
                  <span
                    className="media__scope"
                    style={{
                      background: memberOnly
                        ? 'var(--color-neutral-200)'
                        : 'var(--color-accent-200)',
                      color: memberOnly ? 'var(--color-neutral-800)' : 'var(--color-accent-800)',
                    }}
                  >
                    {m.visibility}
                  </span>
                  {canManage(m) && (
                    <span className="media__actions">
                      <button
                        type="button"
                        className="media__act"
                        onClick={guard(() => setEditing(m))}
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        className="media__act media__act--danger"
                        onClick={guard(() => {
                          if (confirm('이 영상을 삭제할까요?')) void removeMedia(m.id);
                        })}
                      >
                        삭제
                      </button>
                    </span>
                  )}
                </div>
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

      {(addOpen || editing) && (
        <AddMediaModal
          bandId={bandId}
          schedules={schedules}
          editing={editing ?? undefined}
          onClose={() => {
            setAddOpen(false);
            setEditing(null);
          }}
          onSubmitted={() => {
            setAddOpen(false);
            setEditing(null);
            setFilter('전체');
          }}
        />
      )}
    </div>
  );
}
