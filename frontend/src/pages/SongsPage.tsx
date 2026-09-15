import { useEffect, useMemo, useState } from 'react';
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useDroppable,
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
import * as exploreApi from '../api/explore';
import * as mediaApi from '../api/media';
import type { ExploreVideoDto } from '../api/types';
import type { MediaItem, Song, SongFolder } from '../types';
import { Fab } from '../components/Fab';
import { AddSongModal } from '../components/AddSongModal';
import { Modal } from '../components/Modal';
import { PromptModal } from '../components/PromptModal';
import { GuestPickerModal } from '../components/GuestPickerModal';
import './SongsPage.css';

const ROLE_LABEL: Record<string, string> = { owner: '관리자', member: '사용자', guest: '비회원' };
type SortKey = 'manual' | 'votes' | 'recent';
const SORT_LABEL: Record<SortKey, string> = { manual: '수동', votes: '득표순', recent: '최신순' };
const UNFILED = '__unfiled__';

interface AssignOption {
  value: string;
  label: string;
}

/** "u:3" → { userId:'3' }, "g:7" → { guestId:'7' }, "" → null (배정 해제) */
function parseAssignee(value: string): { userId: string } | { guestId: string } | null {
  if (value.startsWith('u:')) return { userId: value.slice(2) };
  if (value.startsWith('g:')) return { guestId: value.slice(2) };
  return null;
}

/** 곡의 슬롯에 지금 배정된 대상을 select value("u:.." / "g:.." / "")로. */
function currentAssignValue(song: Song, slotKey: string): string {
  const part = song.parts.find((p) => `${p.instrument}#${p.partIndex}` === slotKey);
  if (part?.assigneeId) return `u:${part.assigneeId}`;
  if (part?.assigneeGuestId) return `g:${part.assigneeGuestId}`;
  return '';
}

interface Group {
  key: string;
  folder: SongFolder | null;
  songs: Song[];
}

export function SongsPage() {
  const {
    currentBand,
    role,
    songs,
    songFolders,
    members,
    guests,
    media,
    voteSong,
    promoteSong,
    assignPart,
    addGuest,
    moveSongToFolder,
    reorderSongs,
    createSongFolder,
    renameSongFolder,
    removeSongFolder,
    reorderSongFolders,
  } = useApp();
  const guard = useGuard();
  const isOwner = role === 'owner';
  const isGuest = role === 'guest';

  const [tab, setTab] = useState<Song['status']>('WISHLIST');
  const [sort, setSort] = useState<SortKey>('manual');
  const [openId, setOpenId] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [prompt, setPrompt] = useState<{ mode: 'create' | 'rename'; folder?: SongFolder } | null>(
    null,
  );
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const [guestPickerOpen, setGuestPickerOpen] = useState(false);
  const [editingSong, setEditingSong] = useState<Song | null>(null);
  const [proposerFilter, setProposerFilter] = useState<string | null>(null);
  const [proposerPickerOpen, setProposerPickerOpen] = useState(false);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  const bandId = currentBand?.id ?? null;
  const collapseKey = bandId ? `bandive.sf.collapsed.${bandId}` : null;

  useEffect(() => {
    if (!collapseKey) return;
    try {
      const raw = localStorage.getItem(collapseKey);
      setCollapsed(new Set(raw ? (JSON.parse(raw) as string[]) : []));
    } catch {
      setCollapsed(new Set());
    }
  }, [collapseKey]);

  const toggleCollapse = (id: string) =>
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      try {
        if (collapseKey) localStorage.setItem(collapseKey, JSON.stringify([...next]));
      } catch {
        /* localStorage 불가 — 이번 세션만 유지 */
      }
      return next;
    });

  const isWish = tab === 'WISHLIST';

  /** 파트 배정 선택지 — 실멤버 + 게스트. value 는 "u:<id>" / "g:<id>". */
  const assignOptions = useMemo<AssignOption[]>(
    () => [
      ...members
        .filter((m) => m.bandId === bandId)
        .map((m) => ({ value: `u:${m.id}`, label: m.name })),
      ...guests.map((g) => ({
        value: `g:${g.id}`,
        label: `${g.name} · 게스트${g.session ? ` (${g.session})` : ''}`,
      })),
    ],
    [members, guests, bandId],
  );

  const folders = useMemo(
    () => songFolders.filter((f) => f.status === tab).sort((a, b) => a.position - b.position),
    [songFolders, tab],
  );

  /** 곡 id → 연결된 영상들 (최근 등록 순). */
  const mediaBySong = useMemo(() => {
    const map = new Map<string, MediaItem[]>();
    for (const m of media) {
      if (m.bandId !== bandId || m.songId == null) continue;
      const arr = map.get(m.songId) ?? [];
      arr.push(m);
      map.set(m.songId, arr);
    }
    for (const arr of map.values()) arr.sort((a, b) => b.createdAtMs - a.createdAtMs);
    return map;
  }, [media, bandId]);

  const tabSongsAll = useMemo(
    () => songs.filter((s) => s.bandId === bandId && s.status === tab),
    [songs, bandId, tab],
  );
  const tabSongs = useMemo(
    () => tabSongsAll.filter((s) => !proposerFilter || s.addedByUserId === proposerFilter),
    [tabSongsAll, proposerFilter],
  );

  /** 제안자 필터 선택지 — 이 밴드 멤버별로 현재 탭(위시/합주)에서 제안한 곡 수. */
  const proposerCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const s of tabSongsAll) {
      counts.set(s.addedByUserId, (counts.get(s.addedByUserId) ?? 0) + 1);
    }
    return counts;
  }, [tabSongsAll]);
  const wishCount = useMemo(
    () => songs.filter((s) => s.bandId === bandId && s.status === 'WISHLIST').length,
    [songs, bandId],
  );
  const confirmedCount = useMemo(
    () => songs.filter((s) => s.bandId === bandId && s.status === 'CONFIRMED').length,
    [songs, bandId],
  );

  const sortSongs = (list: Song[]) => {
    const arr = [...list];
    if (sort === 'manual') return arr.sort((a, b) => a.position - b.position);
    if (sort === 'votes')
      return arr.sort((a, b) => b.votes - a.votes || b.addedOrder - a.addedOrder);
    return arr.sort((a, b) => b.addedOrder - a.addedOrder);
  };

  const groups: Group[] = [
    ...folders.map((f) => ({
      key: f.id,
      folder: f,
      songs: sortSongs(tabSongs.filter((s) => s.folderId === f.id)),
    })),
    { key: UNFILED, folder: null, songs: sortSongs(tabSongs.filter((s) => s.folderId == null)) },
  ];

  // 제안자로 걸러진 상태에서는 그룹의 일부만 보이므로 드래그 순서변경을 끈다(전체 순서와 안 맞아 서버가 거부함).
  const dragEnabled = sort === 'manual' && !isGuest && !proposerFilter;

  if (!currentBand || !bandId) return null;

  const groupOf = (songId: string) => groups.find((g) => g.songs.some((s) => s.id === songId));
  const folderIdOf = (g: Group) => (g.folder ? g.folder.id : null);

  const onDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (!over) return;
    const activeId = String(active.id);
    const overId = String(over.id);
    if (activeId === overId) return;

    // ── 폴더 순서 이동 ──
    if (activeId.startsWith('F:')) {
      if (!overId.startsWith('F:')) return;
      const ids = folders.map((f) => f.id);
      const from = ids.indexOf(activeId.slice(2));
      const to = ids.indexOf(overId.slice(2));
      if (from < 0 || to < 0) return;
      void reorderSongFolders(tab, arrayMove(ids, from, to));
      return;
    }

    // ── 곡 이동/정렬 ──
    if (!activeId.startsWith('S:')) return;
    const songId = activeId.slice(2);
    const src = groupOf(songId);
    if (!src) return;

    let dest: Group | undefined;
    let destIndex: number;
    if (overId.startsWith('S:')) {
      const overSongId = overId.slice(2);
      dest = groupOf(overSongId);
      destIndex = dest ? dest.songs.findIndex((s) => s.id === overSongId) : -1;
    } else if (overId.startsWith('G:')) {
      const key = overId.slice(2);
      dest = groups.find((g) => g.key === key);
      destIndex = dest ? dest.songs.length : -1;
    } else if (overId.startsWith('F:')) {
      const fid = overId.slice(2);
      dest = groups.find((g) => g.folder?.id === fid);
      destIndex = dest ? dest.songs.length : -1;
    } else {
      return;
    }
    if (!dest || destIndex < 0) return;

    const destFolderId = folderIdOf(dest);

    if (src.key === dest.key) {
      const ids = src.songs.map((s) => s.id);
      const from = ids.indexOf(songId);
      if (from === destIndex) return;
      void reorderSongs(tab, destFolderId, arrayMove(ids, from, destIndex));
      return;
    }

    // 다른 그룹으로: 폴더 이동 → 대상/원본 순서 확정
    const destIds = dest.songs.map((s) => s.id);
    destIds.splice(destIndex, 0, songId);
    const srcIds = src.songs.filter((s) => s.id !== songId).map((s) => s.id);
    void (async () => {
      try {
        await moveSongToFolder(songId, destFolderId);
        await reorderSongs(tab, destFolderId, destIds);
        if (srcIds.length) await reorderSongs(tab, folderIdOf(src), srcIds);
      } catch {
        /* AppContext 가 실패 시 목록 재동기화 */
      }
    })();
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
            위시리스트 {wishCount}
          </button>
          <button
            type="button"
            className={`seg__opt${!isWish ? ' seg__opt--on' : ''}`}
            onClick={() => {
              setTab('CONFIRMED');
              setOpenId(null);
            }}
          >
            합주곡 {confirmedCount}
          </button>
        </div>

        <div className="songs__toolbar">
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <div className="songs__sort">
              <span className="muted" style={{ fontSize: 11 }}>
                정렬
              </span>
              {(['manual', 'votes', 'recent'] as SortKey[]).map((k) => (
                <button
                  key={k}
                  type="button"
                  className={`songs__sortchip${sort === k ? ' is-on' : ''}`}
                  onClick={() => setSort(k)}
                >
                  {SORT_LABEL[k]}
                </button>
              ))}
            </div>
            <div className="songs__sort">
              <span className="muted" style={{ fontSize: 11 }}>
                제안자
              </span>
              <button
                type="button"
                className={`songs__sortchip${proposerFilter ? ' is-on' : ''}`}
                onClick={() => setProposerPickerOpen(true)}
              >
                {proposerFilter
                  ? (members.find((m) => m.id === proposerFilter)?.name ?? '전체')
                  : '전체'}
              </button>
              {proposerFilter && (
                <button
                  type="button"
                  className="songs__sortchip"
                  aria-label="제안자 필터 해제"
                  onClick={() => setProposerFilter(null)}
                >
                  ✕
                </button>
              )}
            </div>
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

        {proposerFilter && !isGuest && (
          <span className="muted" style={{ fontSize: 11 }}>
            제안자로 걸러보는 중 — 순서를 바꾸려면 필터를 해제하세요.
          </span>
        )}
        {!proposerFilter && sort !== 'manual' && !isGuest && (
          <span className="muted" style={{ fontSize: 11 }}>
            {SORT_LABEL[sort]} 보기 중 — 순서를 바꾸려면 ‘수동’을 선택하세요.
          </span>
        )}
        {dragEnabled && (
          <span className="muted" style={{ fontSize: 11 }}>
            <span className="songs__draghint">⠿</span> 손잡이를 끌어 순서를 바꾸거나 다른 폴더로
            옮기세요.
          </span>
        )}
      </header>

      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
        <SortableContext
          items={folders.map((f) => `F:${f.id}`)}
          strategy={verticalListSortingStrategy}
        >
          <div className="songs__list">
            {groups.map((g) => (
              <FolderGroup
                key={g.key}
                group={g}
                isOwner={isOwner}
                isGuest={isGuest}
                isWish={isWish}
                dragEnabled={dragEnabled}
                collapsed={g.folder ? collapsed.has(g.folder.id) : false}
                folders={folders}
                openId={openId}
                assignOptions={assignOptions}
                mediaBySong={mediaBySong}
                canAddGuest={isOwner}
                onToggleCollapse={() => g.folder && toggleCollapse(g.folder.id)}
                onToggle={(id) => setOpenId((cur) => (cur === id ? null : id))}
                onVote={(id) => guard(() => voteSong(id))()}
                onPromote={(id) => {
                  void promoteSong(id);
                  setTab('CONFIRMED');
                  setOpenId(id);
                }}
                onAssign={(songId, slotKey, value) =>
                  void assignPart(songId, slotKey, parseAssignee(value))
                }
                onEditRequest={setEditingSong}
                onAddGuestClick={() => setGuestPickerOpen(true)}
                onMove={moveSongToFolder}
                onRenameRequest={(f) => setPrompt({ mode: 'rename', folder: f })}
                onDelete={removeSongFolder}
              />
            ))}

            {tabSongs.length === 0 && (
              <div className="songs__empty">
                {proposerFilter
                  ? '이 멤버가 제안한 곡이 없습니다.'
                  : isWish
                    ? '아직 위시리스트에 곡이 없습니다.'
                    : '아직 승격된 합주곡이 없습니다.'}
              </div>
            )}
          </div>
        </SortableContext>
      </DndContext>

      <Fab label="＋ 곡 추가" onClick={guard(() => setAddOpen(true))} />

      {addOpen && (
        <AddSongModal
          bandId={bandId}
          defaultConfirmed={isOwner && tab === 'CONFIRMED'}
          onClose={() => setAddOpen(false)}
          onSubmitted={() => {
            setAddOpen(false);
            // 합주곡으로 바로 등록했으면 합주곡 탭에 그대로, 아니면 위시리스트로.
            setTab(isOwner && tab === 'CONFIRMED' ? 'CONFIRMED' : 'WISHLIST');
            setSort('manual');
          }}
        />
      )}

      {editingSong && (
        <AddSongModal
          bandId={bandId}
          editing={editingSong}
          onClose={() => setEditingSong(null)}
          onSubmitted={() => setEditingSong(null)}
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

      {guestPickerOpen && (
        <GuestPickerModal
          guests={guests}
          addedGuestIds={[]}
          onAddNew={addGuest}
          onPick={async () => {}}
          onClose={() => setGuestPickerOpen(false)}
        />
      )}

      {proposerPickerOpen && (
        <Modal title="제안자별 보기" width={320} onClose={() => setProposerPickerOpen(false)}>
          <div className="stack" style={{ gap: 4 }}>
            <button
              type="button"
              className={`songs__proposer-row${proposerFilter === null ? ' is-on' : ''}`}
              onClick={() => {
                setProposerFilter(null);
                setProposerPickerOpen(false);
              }}
            >
              <span>전체</span>
              <span className="muted">{tabSongsAll.length}곡</span>
            </button>
            {members
              .filter((m) => m.bandId === bandId)
              .sort(
                (a, b) =>
                  (proposerCounts.get(b.id) ?? 0) - (proposerCounts.get(a.id) ?? 0) ||
                  a.name.localeCompare(b.name),
              )
              .map((m) => (
                <button
                  key={m.id}
                  type="button"
                  className={`songs__proposer-row${proposerFilter === m.id ? ' is-on' : ''}`}
                  onClick={() => {
                    setProposerFilter(m.id);
                    setProposerPickerOpen(false);
                  }}
                >
                  <span>{m.name}</span>
                  <span className="muted">{proposerCounts.get(m.id) ?? 0}곡</span>
                </button>
              ))}
          </div>
        </Modal>
      )}
    </div>
  );
}

/* ───────────────────────── 폴더 그룹 ───────────────────────── */

interface GroupProps {
  group: Group;
  isOwner: boolean;
  isGuest: boolean;
  isWish: boolean;
  dragEnabled: boolean;
  collapsed: boolean;
  folders: SongFolder[];
  openId: string | null;
  assignOptions: AssignOption[];
  mediaBySong: Map<string, MediaItem[]>;
  canAddGuest: boolean;
  onToggleCollapse: () => void;
  onToggle: (id: string) => void;
  onVote: (id: string) => void;
  onPromote: (id: string) => void;
  onAssign: (songId: string, slotKey: string, value: string) => void;
  onEditRequest: (song: Song) => void;
  onAddGuestClick: () => void;
  onMove: (songId: string, folderId: string | null) => void;
  onRenameRequest: (folder: SongFolder) => void;
  onDelete: (folderId: string) => void;
}

function FolderGroup({
  group,
  isOwner,
  isGuest,
  isWish,
  dragEnabled,
  collapsed,
  folders,
  openId,
  assignOptions,
  mediaBySong,
  canAddGuest,
  onToggleCollapse,
  onToggle,
  onVote,
  onPromote,
  onAssign,
  onEditRequest,
  onAddGuestClick,
  onMove,
  onRenameRequest,
  onDelete,
}: GroupProps) {
  const { folder, songs, key } = group;
  const [confirmDelete, setConfirmDelete] = useState(false);
  const sortable = useSortable({
    id: `F:${folder ? folder.id : UNFILED}`,
    disabled: !folder || !isOwner,
  });
  const droppable = useDroppable({ id: `G:${key}` });
  const style = folder
    ? { transform: CSS.Transform.toString(sortable.transform), transition: sortable.transition }
    : undefined;

  return (
    <section
      ref={folder ? sortable.setNodeRef : undefined}
      style={style}
      className={`folder${sortable.isDragging ? ' is-dragging' : ''}`}
    >
      <div
        ref={droppable.setNodeRef}
        className={`folder__body${droppable.isOver ? ' is-over' : ''}`}
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
          <button
            type="button"
            className="folder__toggle"
            onClick={onToggleCollapse}
            disabled={!folder}
          >
            <span className="folder__caret">{folder ? (collapsed ? '▸' : '▾') : ''}</span>
            <strong className="folder__name">{folder ? folder.name : '미분류'}</strong>
            <span className="folder__count">{songs.length}</span>
          </button>
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

        {collapsed ? (
          <button type="button" className="folder__collapsed" onClick={onToggleCollapse}>
            곡 {songs.length}개 · 펼치기
          </button>
        ) : songs.length === 0 ? (
          <div className="folder__empty">
            {folder ? '이 폴더에 곡이 없습니다.' : '분류 안 된 곡이 없습니다.'}
          </div>
        ) : (
          <SortableContext
            items={songs.map((s) => `S:${s.id}`)}
            strategy={verticalListSortingStrategy}
          >
            {songs.map((song, i) => (
              <SongRow
                key={song.id}
                song={song}
                index={i}
                isWish={isWish}
                isOwner={isOwner}
                isGuest={isGuest}
                dragEnabled={dragEnabled}
                open={openId === song.id}
                assignOptions={assignOptions}
                linkedMedia={mediaBySong.get(song.id) ?? []}
                canAddGuest={canAddGuest}
                folders={folders}
                onToggle={() => onToggle(song.id)}
                onVote={() => onVote(song.id)}
                onPromote={() => onPromote(song.id)}
                onEdit={() => onEditRequest(song)}
                onAssign={(slotKey, value) => onAssign(song.id, slotKey, value)}
                onAddGuestClick={onAddGuestClick}
                onMove={(folderId) => onMove(song.id, folderId)}
              />
            ))}
          </SortableContext>
        )}
      </div>
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
  dragEnabled: boolean;
  open: boolean;
  assignOptions: AssignOption[];
  linkedMedia: MediaItem[];
  canAddGuest: boolean;
  folders: SongFolder[];
  onToggle: () => void;
  onVote: () => void;
  onPromote: () => void;
  onEdit: () => void;
  onAssign: (slotKey: string, value: string) => void;
  onAddGuestClick: () => void;
  onMove: (folderId: string | null) => void;
}

function SongRow({
  song,
  index,
  isWish,
  isOwner,
  isGuest,
  dragEnabled,
  open,
  assignOptions,
  linkedMedia,
  canAddGuest,
  folders,
  onToggle,
  onVote,
  onPromote,
  onEdit,
  onAssign,
  onAddGuestClick,
  onMove,
}: RowProps) {
  const { user, openLogin } = useApp();
  const isMine = user != null && user.id === song.addedByUserId;
  const chips = sessionChips(song);
  const hasRef = song.referenceVideoUrl.length > 0;
  const slots = slotsOf(song);
  const canAssign = song.status === 'CONFIRMED' && !isGuest;
  const showAssignReadonly = song.status === 'CONFIRMED' && isGuest;

  // 다른 밴드가 공개한 같은 곡 합주 영상 (검색으로 추가된 합주곡만)
  const canShowCovers =
    song.status === 'CONFIRMED' && song.sourceType === 'SEARCH' && !!song.externalTrackId;
  const [coversOpen, setCoversOpen] = useState(false);
  const [covers, setCovers] = useState<ExploreVideoDto[] | null>(null);
  const [coversLoading, setCoversLoading] = useState(false);

  const toggleCovers = async () => {
    const next = !coversOpen;
    setCoversOpen(next);
    if (next && covers === null && song.externalTrackId) {
      setCoversLoading(true);
      try {
        setCovers(await exploreApi.exploreSongVideos(song.externalTrackId, song.bandId));
      } catch {
        setCovers([]);
      } finally {
        setCoversLoading(false);
      }
    }
  };

  const toggleCoverLike = async (v: ExploreVideoDto) => {
    if (!user) {
      openLogin();
      return;
    }
    try {
      const res = v.likedByMe
        ? await mediaApi.unlikeMedia(String(v.mediaId))
        : await mediaApi.likeMedia(String(v.mediaId));
      setCovers((prev) =>
        (prev ?? []).map((x) =>
          x.mediaId === v.mediaId
            ? { ...x, likeCount: res.likeCount, likedByMe: res.likedByMe }
            : x,
        ),
      );
    } catch {
      /* 무시 */
    }
  };

  const sortable = useSortable({ id: `S:${song.id}`, disabled: !dragEnabled });
  const style = {
    transform: CSS.Transform.toString(sortable.transform),
    transition: sortable.transition,
  };

  return (
    <article
      ref={sortable.setNodeRef}
      style={style}
      className={`songrow${sortable.isDragging ? ' is-dragging' : ''}`}
    >
      {dragEnabled && (
        <button
          type="button"
          className="songrow__handle"
          aria-label="곡 순서 이동"
          {...sortable.attributes}
          {...sortable.listeners}
        >
          ⠿
        </button>
      )}

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

      <div className="songrow__art">
        {song.artworkUrl ? (
          <img src={song.artworkUrl} alt="" loading="lazy" />
        ) : (
          <svg
            className="songrow__art-fallback"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.7}
            aria-hidden="true"
          >
            <path d="M9 18V5l12-2v13" />
            <circle cx="6" cy="18" r="3" />
            <circle cx="18" cy="16" r="3" />
          </svg>
        )}
      </div>

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
          {linkedMedia.length > 0 && (
            <span className="songrow__chip songrow__chip--ref">영상 {linkedMedia.length}</span>
          )}
        </div>

        {!isGuest && folders.length > 0 && (
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
                <div className="spread">
                  <span className="kicker">파트 배정 · 미지정 가능</span>
                  {canAddGuest && (
                    <button type="button" className="songrow__guest-add" onClick={onAddGuestClick}>
                      ＋ 게스트
                    </button>
                  )}
                </div>
                <div className="songrow__slots">
                  {slots.map((slot) => (
                    <label key={slot.key} className="songrow__slot">
                      <span className="muted">{slot.label}</span>
                      <select
                        className="songrow__select"
                        value={currentAssignValue(song, slot.key)}
                        onChange={(e) => onAssign(slot.key, e.target.value)}
                      >
                        <option value="">미지정</option>
                        {assignOptions.map((o) => (
                          <option key={o.value} value={o.value}>
                            {o.label}
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

            {linkedMedia.length > 0 && (
              <div className="stack" style={{ gap: 6 }}>
                <span className="kicker">연결된 영상 {linkedMedia.length}</span>
                {linkedMedia.map((m) => (
                  <a
                    key={m.id}
                    className="songrow__media"
                    href={m.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {m.thumbnailUrl ? (
                      <img
                        className="songrow__media-thumb"
                        src={m.thumbnailUrl}
                        alt=""
                        loading="lazy"
                      />
                    ) : (
                      <span className="songrow__media-thumb songrow__media-thumb--empty" />
                    )}
                    <span className="stack" style={{ gap: 2, minWidth: 0, flex: 1 }}>
                      <strong style={{ fontSize: 12, wordBreak: 'break-all' }}>{m.title}</strong>
                      <span className="muted" style={{ fontSize: 10 }}>
                        {m.kind} · {m.date} · {m.source}
                      </span>
                    </span>
                  </a>
                ))}
              </div>
            )}

            {canShowCovers && (
              <div className="stack" style={{ gap: 6 }}>
                <button type="button" className="songrow__covers-toggle" onClick={toggleCovers}>
                  <span className="kicker">
                    다른 밴드 합주 영상
                    {covers !== null && covers.length > 0 ? ` ${covers.length}` : ''}
                  </span>
                  <span className="songrow__caret">{coversOpen ? '닫기 ▲' : '보기 ▼'}</span>
                </button>

                {coversOpen && (
                  <>
                    {coversLoading && (
                      <span className="muted" style={{ fontSize: 11 }}>
                        불러오는 중…
                      </span>
                    )}
                    {!coversLoading && covers !== null && covers.length === 0 && (
                      <span className="muted" style={{ fontSize: 11 }}>
                        아직 이 곡을 전체공개한 다른 밴드가 없어요.
                      </span>
                    )}
                    {(covers ?? []).map((v) => (
                      <div key={v.mediaId} className="songrow__cover">
                        <a
                          className="songrow__media"
                          href={v.url}
                          target="_blank"
                          rel="noreferrer"
                          style={{ flex: 1, minWidth: 0 }}
                        >
                          {v.thumbnailUrl ? (
                            <img
                              className="songrow__media-thumb"
                              src={v.thumbnailUrl}
                              alt=""
                              loading="lazy"
                            />
                          ) : (
                            <span className="songrow__media-thumb songrow__media-thumb--empty" />
                          )}
                          <span className="stack" style={{ gap: 2, minWidth: 0, flex: 1 }}>
                            <strong style={{ fontSize: 12 }}>{v.bandName}</strong>
                            {v.title && (
                              <span className="muted" style={{ fontSize: 10 }}>
                                {v.title}
                              </span>
                            )}
                          </span>
                        </a>
                        <button
                          type="button"
                          className={`songrow__cover-like${v.likedByMe ? ' is-liked' : ''}`}
                          onClick={() => toggleCoverLike(v)}
                        >
                          ♥ {v.likeCount}
                        </button>
                      </div>
                    ))}
                  </>
                )}
              </div>
            )}
          </div>
        )}

        {(isOwner || isMine) && (
          <div className="songrow__actions">
            {isWish && isOwner && (
              <button type="button" className="btn btn--primary btn--sm" onClick={onPromote}>
                합주곡으로 승격
              </button>
            )}
            <button type="button" className="btn btn--sm" onClick={onEdit}>
              수정
            </button>
            {isWish && (
              <button type="button" className="btn btn--sm" onClick={onToggle}>
                {open ? '접기' : '상세'}
              </button>
            )}
          </div>
        )}
      </div>
    </article>
  );
}
