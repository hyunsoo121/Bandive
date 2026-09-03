import { useMemo, useState } from 'react';
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
  arrayMove,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { sessionChips, slotsOf } from '../lib/songs';
import type { Song, SongFolder } from '../types';
import { Fab } from '../components/Fab';
import { AddSongModal } from '../components/AddSongModal';
import { PromptModal } from '../components/PromptModal';
import './SongsPage.css';

const ROLE_LABEL: Record<string, string> = { owner: '밴드장', member: '사용자', guest: '비회원' };
type SortKey = 'votes' | 'recent';
const UNFILED = '__unfiled__';

export function SongsPage() {
  const {
    currentBand,
    role,
    songs,
    songFolders,
    members,
    voteSong,
    promoteSong,
    assignPart,
    moveSongToFolder,
    createSongFolder,
    renameSongFolder,
    removeSongFolder,
    reorderSongFolders,
  } = useApp();
  const guard = useGuard();
  const isOwner = role === 'owner';
  const isGuest = role === 'guest';

  const [tab, setTab] = useState<Song['status']>('WISHLIST');
  const [sort, setSort] = useState<SortKey>('votes');
  const [openId, setOpenId] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [prompt, setPrompt] = useState<{ mode: 'create' | 'rename'; folder?: SongFolder } | null>(
    null,
  );

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  if (!currentBand) return null;
  const bandId = currentBand.id;

  const memberNames = members.filter((m) => m.bandId === bandId).map((m) => m.name);
  const bandSongs = songs.filter((s) => s.bandId === bandId);
  const wishlist = bandSongs.filter((s) => s.status === 'WISHLIST');
  const confirmed = bandSongs.filter((s) => s.status === 'CONFIRMED');
  const isWish = tab === 'WISHLIST';
  const tabSongs = isWish ? wishlist : confirmed;

  const folders = useMemo(
    () => songFolders.filter((f) => f.status === tab).sort((a, b) => a.position - b.position),
    [songFolders, tab],
  );

  const sortSongs = (list: Song[]) =>
    [...list].sort((a, b) =>
      sort === 'votes'
        ? b.votes - a.votes || b.addedOrder - a.addedOrder
        : b.addedOrder - a.addedOrder,
    );

  const groups: { key: string; folder: SongFolder | null; songs: Song[] }[] = [
    ...folders.map((f) => ({
      key: f.id,
      folder: f,
      songs: sortSongs(tabSongs.filter((s) => s.folderId === f.id)),
    })),
    { key: UNFILED, folder: null, songs: sortSongs(tabSongs.filter((s) => s.folderId == null)) },
  ];

  const onDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (!over || active.id === over.id) return;
    const ids = folders.map((f) => f.id);
    const from = ids.indexOf(String(active.id));
    const to = ids.indexOf(String(over.id));
    if (from < 0 || to < 0) return;
    void reorderSongFolders(tab, arrayMove(ids, from, to));
  };

  const submitPrompt = (value: string) => {
    if (prompt?.mode === 'create') void createSongFolder(value, tab);
    else if (prompt?.folder) void renameSongFolder(prompt.folder.id, value);
    setPrompt(null);
  };

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

        <div className="songs__toolbar">
          <div className="songs__sort">
            <span className="muted" style={{ fontSize: 11 }}>
              폴더 안 정렬
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
          {isOwner && (
            <button
              type="button"
              className="btn btn--sm"
              onClick={() => setPrompt({ mode: 'create' })}
            >
              ＋ 폴더
            </button>
          )}
        </div>
      </header>

      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
        <SortableContext items={folders.map((f) => f.id)} strategy={verticalListSortingStrategy}>
          <div className="songs__list">
            {groups.map((g) => (
              <FolderGroup
                key={g.key}
                folder={g.folder}
                songs={g.songs}
                isOwner={isOwner}
                isGuest={isGuest}
                isWish={isWish}
                folders={folders}
                openId={openId}
                memberNames={memberNames}
                onToggle={(id) => setOpenId((cur) => (cur === id ? null : id))}
                onVote={(id) => guard(() => voteSong(id))()}
                onPromote={(id) => {
                  void promoteSong(id);
                  setTab('CONFIRMED');
                  setOpenId(id);
                }}
                onAssign={assignPart}
                onMove={moveSongToFolder}
                onRenameRequest={(f) => setPrompt({ mode: 'rename', folder: f })}
                onDelete={removeSongFolder}
              />
            ))}

            {tabSongs.length === 0 && (
              <div className="songs__empty">
                {isWish ? '아직 위시리스트에 곡이 없습니다.' : '아직 승격된 합주곡이 없습니다.'}
              </div>
            )}
          </div>
        </SortableContext>
      </DndContext>

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

      {prompt && (
        <PromptModal
          title={prompt.mode === 'create' ? '새 곡 폴더' : '폴더 이름 변경'}
          label={
            prompt.mode === 'create' ? `${isWish ? '위시리스트' : '합주곡'} 폴더 이름` : '폴더 이름'
          }
          initial={prompt.folder?.name ?? ''}
          placeholder="예: 커버곡, 5월 공연"
          submitLabel={prompt.mode === 'create' ? '만들기' : '변경'}
          onSubmit={submitPrompt}
          onClose={() => setPrompt(null)}
        />
      )}
    </div>
  );
}

/* ───────────────────────── 폴더 그룹 ───────────────────────── */

interface GroupProps {
  folder: SongFolder | null;
  songs: Song[];
  isOwner: boolean;
  isGuest: boolean;
  isWish: boolean;
  folders: SongFolder[];
  openId: string | null;
  memberNames: string[];
  onToggle: (id: string) => void;
  onVote: (id: string) => void;
  onPromote: (id: string) => void;
  onAssign: (songId: string, slotKey: string, memberName: string) => void;
  onMove: (songId: string, folderId: string | null) => void;
  onRenameRequest: (folder: SongFolder) => void;
  onDelete: (folderId: string) => void;
}

function FolderGroup({
  folder,
  songs,
  isOwner,
  isGuest,
  isWish,
  folders,
  openId,
  memberNames,
  onToggle,
  onVote,
  onPromote,
  onAssign,
  onMove,
  onRenameRequest,
  onDelete,
}: GroupProps) {
  const [confirmDelete, setConfirmDelete] = useState(false);
  const sortable = useSortable({ id: folder ? folder.id : UNFILED, disabled: !folder || !isOwner });
  const style = folder
    ? { transform: CSS.Transform.toString(sortable.transform), transition: sortable.transition }
    : undefined;

  return (
    <section
      ref={folder ? sortable.setNodeRef : undefined}
      style={style}
      className={`folder${sortable.isDragging ? ' is-dragging' : ''}`}
    >
      <div className="folder__head">
        {folder && isOwner && (
          <button
            type="button"
            className="folder__handle"
            aria-label="폴더 순서 이동"
            {...sortable.attributes}
            {...sortable.listeners}
          >
            ⠿
          </button>
        )}
        <strong className="folder__name">{folder ? folder.name : '미분류'}</strong>
        <span className="folder__count">{songs.length}</span>
        {folder && isOwner && (
          <span className="folder__actions">
            <button type="button" className="folder__act" onClick={() => onRenameRequest(folder)}>
              이름
            </button>
            {confirmDelete ? (
              <>
                <button
                  type="button"
                  className="folder__act folder__act--danger"
                  onClick={() => onDelete(folder.id)}
                >
                  정말 삭제
                </button>
                <button
                  type="button"
                  className="folder__act"
                  onClick={() => setConfirmDelete(false)}
                >
                  취소
                </button>
              </>
            ) : (
              <button
                type="button"
                className="folder__act folder__act--danger"
                onClick={() => setConfirmDelete(true)}
                title="곡은 미분류로 이동합니다"
              >
                삭제
              </button>
            )}
          </span>
        )}
      </div>

      {songs.length === 0 ? (
        <div className="folder__empty">
          {folder ? '이 폴더에 곡이 없습니다.' : '분류 안 된 곡이 없습니다.'}
        </div>
      ) : (
        songs.map((song, i) => (
          <SongRow
            key={song.id}
            song={song}
            index={i}
            isWish={isWish}
            isOwner={isOwner}
            isGuest={isGuest}
            open={openId === song.id}
            memberNames={memberNames}
            folders={folders}
            onToggle={() => onToggle(song.id)}
            onVote={() => onVote(song.id)}
            onPromote={() => onPromote(song.id)}
            onAssign={(slotKey, name) => onAssign(song.id, slotKey, name)}
            onMove={(folderId) => onMove(song.id, folderId)}
          />
        ))
      )}
    </section>
  );
}

/* ───────────────────────── 곡 행 ───────────────────────── */

interface RowProps {
  song: Song;
  index: number;
  isWish: boolean;
  isOwner: boolean;
  isGuest: boolean;
  open: boolean;
  memberNames: string[];
  folders: SongFolder[];
  onToggle: () => void;
  onVote: () => void;
  onPromote: () => void;
  onAssign: (slotKey: string, memberName: string) => void;
  onMove: (folderId: string | null) => void;
}

function SongRow({
  song,
  index,
  isWish,
  isOwner,
  isGuest,
  open,
  memberNames,
  folders,
  onToggle,
  onVote,
  onPromote,
  onAssign,
  onMove,
}: RowProps) {
  const chips = sessionChips(song);
  const hasRef = song.referenceVideoUrl.length > 0;
  const slots = slotsOf(song);
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

      {song.artworkUrl && (
        <img className="songrow__art" src={song.artworkUrl} alt="" loading="lazy" />
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

        {isOwner && folders.length > 0 && (
          <label className="songrow__folder">
            <span className="muted" style={{ fontSize: 11 }}>
              폴더
            </span>
            <select
              className="songrow__folder-select"
              value={song.folderId ?? ''}
              onChange={(e) => onMove(e.target.value || null)}
            >
              <option value="">미분류</option>
              {folders.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </select>
          </label>
        )}

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
