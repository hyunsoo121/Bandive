import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { sessionChips, slotsOf } from '../lib/songs';
import type { Song } from '../types';
import { Fab } from '../components/Fab';
import { AddSongModal } from '../components/AddSongModal';
import './SongsPage.css';

const ROLE_LABEL: Record<string, string> = { owner: '밴드장', member: '사용자', guest: '비회원' };

type SortKey = 'votes' | 'recent';

export function SongsPage() {
  const { currentBand, role, songs, members, voteSong, promoteSong, assignPart } = useApp();
  const guard = useGuard();
  const bandId = currentBand.id;
  const isOwner = role === 'owner';
  const isGuest = role === 'guest';

  const [tab, setTab] = useState<Song['status']>('WISHLIST');
  const [sort, setSort] = useState<SortKey>('votes');
  const [openId, setOpenId] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);

  const memberNames = members.filter((m) => m.bandId === bandId).map((m) => m.name);

  const bandSongs = songs.filter((s) => s.bandId === bandId);
  const wishlist = bandSongs.filter((s) => s.status === 'WISHLIST');
  const confirmed = bandSongs.filter((s) => s.status === 'CONFIRMED');
  const isWish = tab === 'WISHLIST';

  const list = isWish
    ? [...wishlist].sort((a, b) =>
        sort === 'votes'
          ? b.votes - a.votes || b.addedOrder - a.addedOrder
          : b.addedOrder - a.addedOrder,
      )
    : [...confirmed].sort((a, b) => a.addedOrder - b.addedOrder);

  return (
    <div className="songs">
      <header className="songs__head">
        <div className="spread">
          <h2>곡</h2>
          <span className="songs__role">{ROLE_LABEL[role]}</span>
        </div>

        <div className="seg">
          <button
            type="button"
            className={`seg__opt${isWish ? ' seg__opt--on' : ''}`}
            onClick={() => {
              setTab('WISHLIST');
              setOpenId(null);
            }}
          >
            위시리스트 {wishlist.length}
          </button>
          <button
            type="button"
            className={`seg__opt${!isWish ? ' seg__opt--on' : ''}`}
            onClick={() => {
              setTab('CONFIRMED');
              setOpenId(null);
            }}
          >
            합주곡 {confirmed.length}
          </button>
        </div>

        {isWish && (
          <div className="songs__sort">
            <span className="muted" style={{ fontSize: 11 }}>
              정렬
            </span>
            {(['votes', 'recent'] as SortKey[]).map((k) => (
              <button
                key={k}
                type="button"
                className={`songs__sortchip${sort === k ? ' is-on' : ''}`}
                onClick={() => setSort(k)}
              >
                {k === 'votes' ? '득표순' : '최신순'}
              </button>
            ))}
          </div>
        )}
      </header>

      <div className="songs__list">
        {list.map((song, i) => (
          <SongRow
            key={song.id}
            song={song}
            index={i}
            isWish={isWish}
            isOwner={isOwner}
            isGuest={isGuest}
            open={openId === song.id}
            memberNames={memberNames}
            onToggle={() => setOpenId((cur) => (cur === song.id ? null : song.id))}
            onVote={guard(() => voteSong(song.id))}
            onPromote={() => {
              promoteSong(song.id);
              setTab('CONFIRMED');
              setOpenId(song.id);
            }}
            onAssign={(slotKey, name) => assignPart(song.id, slotKey, name)}
          />
        ))}

        {list.length === 0 && (
          <div className="songs__empty">
            {isWish ? '아직 위시리스트에 곡이 없습니다.' : '아직 승격된 합주곡이 없습니다.'}
          </div>
        )}
      </div>

      <Fab label="＋ 곡 추가" onClick={guard(() => setAddOpen(true))} />

      {addOpen && (
        <AddSongModal
          bandId={bandId}
          onClose={() => setAddOpen(false)}
          onSubmitted={() => {
            setAddOpen(false);
            setTab('WISHLIST');
            setSort('recent');
          }}
        />
      )}
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────── */

interface RowProps {
  song: Song;
  index: number;
  isWish: boolean;
  isOwner: boolean;
  isGuest: boolean;
  open: boolean;
  memberNames: string[];
  onToggle: () => void;
  onVote: () => void;
  onPromote: () => void;
  onAssign: (slotKey: string, memberName: string) => void;
}

function SongRow({
  song,
  index,
  isWish,
  isOwner,
  isGuest,
  open,
  memberNames,
  onToggle,
  onVote,
  onPromote,
  onAssign,
}: RowProps) {
  const chips = sessionChips(song);
  const hasRef = song.referenceVideoUrl.length > 0;
  const slots = slotsOf(song);
  // 파트 배정: 로그인한 멤버 누구나 (목업 동작). 비회원은 읽기 전용.
  const canAssign = song.status === 'CONFIRMED' && !isGuest;
  const showAssignReadonly = song.status === 'CONFIRMED' && isGuest;

  return (
    <article className="songrow">
      {isWish ? (
        <button
          type="button"
          className={`votebox${song.votedByMe ? ' is-voted' : ''}`}
          onClick={onVote}
          aria-pressed={song.votedByMe}
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={3}
          >
            <path d="m5 15 7-7 7 7" />
          </svg>
          <span className="votebox__count">{song.votes}</span>
          <span className="votebox__label">{song.votedByMe ? '투표함' : '투표'}</span>
        </button>
      ) : (
        <span className="songrow__no">{String(index + 1).padStart(2, '0')}</span>
      )}

      <div className="songrow__main">
        <button type="button" className="songrow__title-btn" onClick={onToggle}>
          <span className="stack" style={{ gap: 4 }}>
            <strong className="songrow__title">{song.title}</strong>
            <span className="muted" style={{ fontSize: 12 }}>
              {song.artist} · {song.proposer} 제안
            </span>
          </span>
          <span className="songrow__caret">{open ? '닫기 ▲' : '상세 ▼'}</span>
        </button>

        <div className="songrow__chips">
          {chips.map((c) => (
            <span key={c} className="songrow__chip">
              {c}
            </span>
          ))}
          {hasRef && <span className="songrow__chip songrow__chip--ref">참고 영상</span>}
        </div>

        {open && (
          <div className="songrow__detail panel">
            {canAssign && (
              <div className="stack" style={{ gap: 7 }}>
                <span className="kicker">파트 배정 · 미지정 가능</span>
                <div className="songrow__slots">
                  {slots.map((slot) => (
                    <label key={slot.key} className="songrow__slot">
                      <span className="muted">{slot.label}</span>
                      <select
                        className="songrow__select"
                        value={song.assignments[slot.key] ?? ''}
                        onChange={(e) => onAssign(slot.key, e.target.value)}
                      >
                        <option value="">미지정</option>
                        {memberNames.map((n) => (
                          <option key={n} value={n}>
                            {n}
                          </option>
                        ))}
                      </select>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {showAssignReadonly && (
              <div className="stack" style={{ gap: 7 }}>
                <span className="kicker">파트 배정</span>
                <div className="songrow__slots">
                  {slots.map((slot) => (
                    <div key={slot.key} className="songrow__slot">
                      <span className="muted">{slot.label}</span>
                      <span className="songrow__assignee">
                        {song.assignments[slot.key] || '미지정'}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {song.status === 'WISHLIST' && (
              <span className="muted" style={{ fontSize: 11, lineHeight: 1.5 }}>
                파트 배정은 합주곡으로 승격된 뒤에 멤버별로 지정할 수 있습니다.
              </span>
            )}

            <div className="stack" style={{ gap: 4 }}>
              <span className="kicker">비고 / 메모</span>
              <span style={{ fontSize: 12, lineHeight: 1.5 }}>
                {song.memo || <span className="muted">메모 없음</span>}
              </span>
            </div>

            {hasRef && (
              <div className="stack" style={{ gap: 4 }}>
                <span className="kicker">참고 영상</span>
                <a
                  href={song.referenceVideoUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{ fontSize: 12, fontWeight: 600, wordBreak: 'break-all' }}
                >
                  {song.referenceVideoUrl}
                </a>
              </div>
            )}
          </div>
        )}

        {isWish && isOwner && (
          <div className="songrow__actions">
            <button type="button" className="btn btn--primary btn--sm" onClick={onPromote}>
              합주곡으로 승격
            </button>
            <button type="button" className="btn btn--sm" onClick={onToggle}>
              {open ? '접기' : '상세'}
            </button>
          </div>
        )}
      </div>
    </article>
  );
}
