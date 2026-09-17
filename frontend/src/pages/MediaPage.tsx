import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { KIND_LABEL, toUi } from '../lib/schedule';
import type { MediaItem, MediaKind, ScheduleEvent } from '../types';
import { Fab } from '../components/Fab';
import { AddMediaModal } from '../components/AddMediaModal';
import './MediaPage.css';

type Filter = '전체' | MediaKind;
const FILTERS: Filter[] = ['전체', '합주', '공연'];

type SortKey = 'recent' | 'oldest' | 'likes' | 'title' | 'schedule';
const SORT_LABEL: Record<SortKey, string> = {
  recent: '최신순',
  oldest: '오래된순',
  likes: '좋아요순',
  title: '제목순',
  schedule: '일정순',
};

const STRIPE_SHADES = [
  ['#9b9797', '#bab6b6'],
  ['#7d7979', '#9b9797'],
  ['#605d5d', '#7d7979'],
  ['#444141', '#605d5d'],
];
const stripe = (a: string, b: string) =>
  `repeating-linear-gradient(135deg, ${a} 0 12px, ${b} 12px 24px)`;

/** 정렬칩 하나로 목록 전체를 정렬 — 고정된 영상은 어떤 정렬을 고르든 그 안에서도 항상 맨 앞. */
function sortMedia(
  list: MediaItem[],
  sort: SortKey,
  scheduleById: Map<string, ScheduleEvent>,
): MediaItem[] {
  const scheduleTimeOf = (m: MediaItem) => {
    const ev = m.scheduleId ? scheduleById.get(m.scheduleId) : undefined;
    return ev ? new Date(ev.dateTime).getTime() : Number.POSITIVE_INFINITY;
  };
  const cmp = (a: MediaItem, b: MediaItem) => {
    switch (sort) {
      case 'recent':
        return b.createdAtMs - a.createdAtMs;
      case 'oldest':
        return a.createdAtMs - b.createdAtMs;
      case 'likes':
        return b.likeCount - a.likeCount || b.createdAtMs - a.createdAtMs;
      case 'title':
        return a.title.localeCompare(b.title, 'ko');
      case 'schedule':
        return scheduleTimeOf(a) - scheduleTimeOf(b);
    }
  };
  return [...list].sort((a, b) => Number(b.pinned) - Number(a.pinned) || cmp(a, b));
}

export function MediaPage() {
  const {
    currentBand,
    role,
    user,
    media: allMedia,
    schedules,
    removeMedia,
    likeMedia,
    togglePinMedia,
  } = useApp();
  const guard = useGuard();
  const isGuest = role === 'guest';
  const isOwner = role === 'owner';

  const [filter, setFilter] = useState<Filter>('전체');
  const [sort, setSort] = useState<SortKey>('recent');
  const [addOpen, setAddOpen] = useState(false);
  const [editing, setEditing] = useState<MediaItem | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const [highlightId, setHighlightId] = useState<string | null>(null);

  const bandId = currentBand?.id;

  // 홈 "최근 영상"에서 넘어온 경우 — 그 영상이 보이게 필터를 풀고 스크롤·하이라이트한다.
  useEffect(() => {
    const targetId = searchParams.get('video');
    if (!targetId || !bandId) return;
    const target = allMedia.find((m) => m.id === targetId && m.bandId === bandId);
    if (!target) return;
    setFilter('전체');
    setHighlightId(target.id);
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.delete('video');
        return next;
      },
      { replace: true },
    );
  }, [searchParams, allMedia, bandId, setSearchParams]);

  useEffect(() => {
    if (!highlightId) return;
    document.getElementById(`media-${highlightId}`)?.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
    });
    const t = setTimeout(() => setHighlightId(null), 1800);
    return () => clearTimeout(t);
  }, [highlightId]);

  if (!currentBand || !bandId) return null;

  const scheduleById = new Map(schedules.map((s) => [s.id, s]));

  const bandMedia = allMedia.filter((m) => m.bandId === bandId);
  // 공개범위 적용: 비회원은 '멤버만' 영상 제외 (기획서 8.7)
  const visible = bandMedia.filter((m) => !isGuest || m.visibility === '전체공개');
  const hiddenCount = bandMedia.length - visible.length;
  const filtered = visible.filter((m) => filter === '전체' || m.kind === filter);
  const list = sortMedia(filtered, sort, scheduleById);

  const canManage = (m: MediaItem) =>
    role === 'owner' || (user != null && m.uploadedByUserId === user.id);

  return (
    <div className="media">
      <header className="media__head">
        <h2>영상</h2>
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

      <div className="media__filters">
        <span className="muted" style={{ fontSize: 11 }}>
          정렬
        </span>
        {(Object.keys(SORT_LABEL) as SortKey[]).map((k) => (
          <button
            key={k}
            type="button"
            className={`media__chip${sort === k ? ' is-on' : ''}`}
            onClick={() => setSort(k)}
          >
            {SORT_LABEL[k]}
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
            <article
              key={m.id}
              id={`media-${m.id}`}
              className={`media__card${highlightId === m.id ? ' is-highlighted' : ''}`}
            >
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
                <span className="media__play" aria-hidden="true" />
                <span className="media__kind">{m.kind}</span>
                {m.pinned && <span className="media__pin">📌 고정</span>}
                {m.platform === 'other' && <span className="media__other">기타 링크</span>}
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
                {evUi ? (
                  <Link
                    className="media__link"
                    style={{ color: 'var(--color-accent-700)' }}
                    to={`/bands/${bandId}/schedule?schedule=${evUi.id}`}
                  >
                    일정 · {evUi.year}. {evUi.month + 1}/{evUi.day} ·{' '}
                    {evUi.title || KIND_LABEL[evUi.type]}
                    {evUi.location ? ` · ${evUi.location}` : ''}
                  </Link>
                ) : (
                  <span className="media__link" style={{ color: 'var(--color-neutral-600)' }}>
                    연결된 일정 없음
                  </span>
                )}
                {m.songTitle && (
                  <span className="media__link" style={{ color: 'var(--color-accent-700)' }}>
                    곡 · {m.songTitle}
                  </span>
                )}
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
                  <span className="media__foot-right">
                    <button
                      type="button"
                      className={`media__like${m.likedByMe ? ' is-liked' : ''}`}
                      onClick={guard(() => void likeMedia(m.id))}
                      aria-pressed={m.likedByMe}
                      title={m.likedByMe ? '좋아요 취소' : '좋아요'}
                    >
                      <svg
                        className="media__like-icon"
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                        fill={m.likedByMe ? 'currentColor' : 'none'}
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                      </svg>
                      <span className="media__like-count">{m.likeCount}</span>
                    </button>
                    {isOwner && (
                      <span className="media__actions">
                        <button
                          type="button"
                          className="media__act"
                          onClick={() => void togglePinMedia(m.id)}
                        >
                          {m.pinned ? '고정 해제' : '고정'}
                        </button>
                      </span>
                    )}
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
                  </span>
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
