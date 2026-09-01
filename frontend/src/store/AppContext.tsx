import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import type {
  Band,
  MediaItem,
  MediaKind,
  Member,
  Role,
  SessionShape,
  Song,
  SourceType,
  User,
  Visibility,
} from '../types';
import { BANDS, CURRENT_USER, MEDIA, MEMBERS, SONGS } from '../mock/data';

export interface NewSongInput {
  bandId: string;
  title: string;
  artist: string;
  sourceType: SourceType;
  memo: string;
  referenceVideoUrl: string;
  sessions: SessionShape;
}

const SEED_INVITE_CODES: Record<string, string> = {
  b1: 'BLT-7K29',
  b2: 'LHX-3M18',
  b3: 'THN-9Q40',
};
const randomInviteCode = () => `BND-${Math.random().toString(36).slice(2, 6).toUpperCase()}`;

export interface NewMediaInput {
  bandId: string;
  url: string;
  title: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결할 일정의 day. 없으면 null */
  scheduleDay: number | null;
}

/**
 * 앱 전역 상태 — 세션(로그인 유저 / 역할), 현재 밴드, 공용 모달.
 * 백엔드가 붙으면 login/logout 은 실제 카카오 OAuth 플로우로, BANDS 는 GET /api/bands/my 로 교체.
 */
interface AppState {
  user: User | null;
  bands: Band[];
  currentBand: Band;
  /** 화면 권한 판정에 쓰는 최종 역할 (게스트 포함) */
  role: Role;
  /** 백엔드 전 프리뷰용 역할 강제 (null 이면 실제 멤버십 기준) */
  devRole: Role | null;

  switcherOpen: boolean;
  loginOpen: boolean;
  createOpen: boolean;

  /** 전체 곡 (모든 밴드). 화면에서 밴드로 필터. → 백엔드 시 GET /api/bands/{id}/songs */
  songs: Song[];
  /** 전체 영상 (모든 밴드). 화면에서 밴드로 필터. → 백엔드 시 GET /api/bands/{id}/media */
  media: MediaItem[];
  /** 전체 멤버 (모든 밴드). 화면에서 밴드로 필터. → 백엔드 시 GET /api/bands/{id}/members */
  members: Member[];
  /** 밴드별 현재 초대 코드. → 백엔드 시 POST /api/bands/{id}/invite-codes */
  inviteCodes: Record<string, string>;

  setCurrentBandId: (id: string) => void;
  setDevRole: (role: Role | null) => void;
  login: () => void;
  logout: () => void;
  createBand: (name: string) => void;

  /** 투표 토글 (1인 1표). 비회원 가드는 호출부(useGuard)에서 */
  voteSong: (songId: string) => void;
  /** 위시리스트 → 합주곡 승격 (밴드장) */
  promoteSong: (songId: string) => void;
  /** 파트 슬롯에 멤버 배정/해제 ('' 이면 해제). 합주곡 상태에서만 */
  assignPart: (songId: string, slotKey: string, memberName: string) => void;
  /** 곡 추가 (위시리스트로) */
  addSong: (input: NewSongInput) => void;
  /** 영상 URL 첨부 */
  addMedia: (input: NewMediaInput) => void;
  /** 멤버 추방 (밴드장, OWNER 는 불가) */
  kickMember: (memberId: string) => void;
  /** 초대 코드 재발급 (밴드장) */
  regenerateInviteCode: (bandId: string) => void;

  openSwitcher: () => void;
  closeSwitcher: () => void;
  openLogin: () => void;
  closeLogin: () => void;
  openCreate: () => void;
  closeCreate: () => void;
}

const AppContext = createContext<AppState | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(CURRENT_USER);
  const [bands, setBands] = useState<Band[]>(BANDS);
  const [currentBandId, setCurrentBandId] = useState<string>(BANDS[0].id);
  const [devRole, setDevRole] = useState<Role | null>(null);

  const [songs, setSongs] = useState<Song[]>(SONGS);
  const [media, setMedia] = useState<MediaItem[]>(MEDIA);
  const [members, setMembers] = useState<Member[]>(MEMBERS);
  const [inviteCodes, setInviteCodes] = useState<Record<string, string>>(SEED_INVITE_CODES);

  const [switcherOpen, setSwitcherOpen] = useState(false);
  const [loginOpen, setLoginOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  const currentBand = useMemo(
    () => bands.find((b) => b.id === currentBandId) ?? bands[0],
    [bands, currentBandId],
  );

  const role: Role = devRole ?? (user ? currentBand.myRole : 'guest');

  const login = useCallback(() => {
    setUser(CURRENT_USER);
    setDevRole((r) => (r === 'guest' ? null : r));
    setLoginOpen(false);
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setDevRole(null);
  }, []);

  const createBand = useCallback((name: string) => {
    const trimmed = name.trim();
    if (!trimmed) return;
    setBands((prev) => {
      const id = `n${prev.length + 1}`;
      const band: Band = {
        id,
        name: trimmed,
        initial: [...trimmed][0] ?? '밴',
        memberCount: 1,
        myRole: 'owner',
        note: '합주곡 0 · 위시 0',
      };
      setCurrentBandId(id);
      setInviteCodes((codes) => ({ ...codes, [id]: randomInviteCode() }));
      return [...prev, band];
    });
    setCreateOpen(false);
    setSwitcherOpen(false);
  }, []);

  const regenerateInviteCode = useCallback((bandId: string) => {
    setInviteCodes((prev) => ({ ...prev, [bandId]: randomInviteCode() }));
  }, []);

  const voteSong = useCallback((songId: string) => {
    setSongs((prev) =>
      prev.map((s) => {
        if (s.id !== songId) return s;
        return s.votedByMe
          ? { ...s, votedByMe: false, votes: Math.max(0, s.votes - 1) }
          : { ...s, votedByMe: true, votes: s.votes + 1 };
      }),
    );
  }, []);

  const promoteSong = useCallback((songId: string) => {
    setSongs((prev) => prev.map((s) => (s.id === songId ? { ...s, status: 'CONFIRMED' } : s)));
  }, []);

  const assignPart = useCallback((songId: string, slotKey: string, memberName: string) => {
    setSongs((prev) =>
      prev.map((s) => {
        if (s.id !== songId) return s;
        const assignments = { ...s.assignments };
        if (memberName) assignments[slotKey] = memberName;
        else delete assignments[slotKey];
        return { ...s, assignments };
      }),
    );
  }, []);

  const addSong = useCallback(
    (input: NewSongInput) => {
      setSongs((prev) => {
        const maxOrder = prev.reduce((m, s) => Math.max(m, s.addedOrder), 0);
        const song: Song = {
          id: `u${Date.now()}`,
          bandId: input.bandId,
          title: input.title.trim(),
          artist: input.artist.trim(),
          status: 'WISHLIST',
          sourceType: input.sourceType,
          proposer: user?.name ?? '게스트',
          memo: input.memo.trim(),
          referenceVideoUrl: input.referenceVideoUrl.trim(),
          sessions: input.sessions,
          votes: 0,
          votedByMe: false,
          addedOrder: maxOrder + 1,
          assignments: {},
        };
        return [...prev, song];
      });
    },
    [user],
  );

  const addMedia = useCallback((input: NewMediaInput) => {
    const url = input.url.trim();
    const source: MediaItem['source'] = /drive\.google/i.test(url) ? 'Google Drive' : 'YouTube';
    const item: MediaItem = {
      id: `um${Date.now()}`,
      bandId: input.bandId,
      title: input.title.trim(),
      source,
      date: '방금 등록',
      kind: input.kind,
      visibility: input.visibility,
      scheduleDay: input.scheduleDay,
    };
    setMedia((prev) => [item, ...prev]);
  }, []);

  const kickMember = useCallback((memberId: string) => {
    const target = MEMBERS.find((m) => m.id === memberId);
    if (!target || target.role === 'owner') return;
    setMembers((prev) => prev.filter((m) => m.id !== memberId));
    setBands((prev) =>
      prev.map((b) =>
        b.id === target.bandId ? { ...b, memberCount: Math.max(1, b.memberCount - 1) } : b,
      ),
    );
  }, []);

  const value: AppState = {
    user,
    bands,
    currentBand,
    role,
    devRole,
    songs,
    media,
    members,
    inviteCodes,
    switcherOpen,
    loginOpen,
    createOpen,
    setCurrentBandId: (id) => {
      setCurrentBandId(id);
      setSwitcherOpen(false);
    },
    setDevRole,
    login,
    logout,
    createBand,
    voteSong,
    promoteSong,
    assignPart,
    addSong,
    addMedia,
    kickMember,
    regenerateInviteCode,
    openSwitcher: () => setSwitcherOpen(true),
    closeSwitcher: () => setSwitcherOpen(false),
    openLogin: () => setLoginOpen(true),
    closeLogin: () => setLoginOpen(false),
    openCreate: () => {
      setCreateOpen(true);
      setSwitcherOpen(false);
    },
    closeCreate: () => setCreateOpen(false),
  };

  return <AppContext value={value}>{children}</AppContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useApp(): AppState {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within <AppProvider>');
  return ctx;
}
