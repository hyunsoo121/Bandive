import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useNavigate } from 'react-router-dom';
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
import { MEDIA, SONGS } from '../mock/data';
import * as authApi from '../api/auth';
import * as bandApi from '../api/bands';
import * as inviteApi from '../api/invites';
import * as memberApi from '../api/members';
import { toBand, toMember, toUser } from '../api/mappers';

export interface NewSongInput {
  bandId: string;
  title: string;
  artist: string;
  sourceType: SourceType;
  memo: string;
  referenceVideoUrl: string;
  sessions: SessionShape;
}

export interface NewMediaInput {
  bandId: string;
  url: string;
  title: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결할 일정의 day. 없으면 null */
  scheduleDay: number | null;
}

/** 현재 밴드의 초대 코드 (밴드장이 발급/재발급한 뒤에만 채워진다 — 조회 전용 API 가 없어서). */
export interface InviteInfo {
  code: string;
  url: string;
}

/**
 * 앱 전역 상태.
 * - 인증(카카오 OAuth) · 밴드 · 멤버 · 초대는 백엔드 API 연동됨.
 * - 곡 · 영상 · 일정은 백엔드 Phase 4-4~4-6 전까지 mock 유지.
 */
interface AppState {
  user: User | null;
  /** 내가 속한 밴드 (GET /api/bands/my). 게스트는 빈 배열 */
  bands: Band[];
  /** URL 의 현재 밴드 id */
  currentBandId: string | null;
  /** 현재 밴드 상세 (GET /api/bands/{id}). 로딩 중이거나 없으면 null */
  currentBand: Band | null;
  /** 세션 복구(첫 refresh) 진행 중 */
  bootLoading: boolean;
  /** 현재 밴드 상세/멤버 로딩 중 */
  bandLoading: boolean;
  /** 화면 권한 판정에 쓰는 최종 역할 (게스트 포함) */
  role: Role;
  /** 개발용 역할 강제 (null 이면 실제 멤버십 기준). 곡/영상 등 mock 화면 프리뷰용 */
  devRole: Role | null;

  switcherOpen: boolean;
  loginOpen: boolean;
  createOpen: boolean;

  /** 전체 곡 (mock). 화면에서 밴드로 필터. → 백엔드 Phase 4-4 */
  songs: Song[];
  /** 전체 영상 (mock). 화면에서 밴드로 필터. → 백엔드 Phase 4-6 */
  media: MediaItem[];
  /** 현재 밴드 멤버 (GET /api/bands/{id}/members) */
  members: Member[];
  /** 현재 밴드 초대 코드 (발급 후에만) */
  invite: InviteInfo | null;

  setCurrentBandId: (id: string) => void;
  setDevRole: (role: Role | null) => void;
  /** 카카오 로그인 시작 (페이지 이동) */
  login: () => void;
  logout: () => void;
  /** 밴드 생성 → 내 밴드에 추가하고 해당 밴드로 이동 */
  createBand: (name: string) => Promise<void>;
  /** 초대 코드로 가입 → 가입한 밴드 반환 */
  joinByInvite: (code: string) => Promise<Band>;

  /** 투표 토글 (mock) */
  voteSong: (songId: string) => void;
  /** 위시리스트 → 합주곡 승격 (mock) */
  promoteSong: (songId: string) => void;
  /** 파트 슬롯 배정/해제 (mock) */
  assignPart: (songId: string, slotKey: string, memberName: string) => void;
  /** 곡 추가 (mock) */
  addSong: (input: NewSongInput) => void;
  /** 영상 URL 첨부 (mock) */
  addMedia: (input: NewMediaInput) => void;
  /** 멤버 추방 (밴드장) — userId */
  kickMember: (userId: string) => Promise<void>;
  /** 초대 코드 발급/재발급 (밴드장) */
  issueInviteCode: () => Promise<void>;

  openSwitcher: () => void;
  closeSwitcher: () => void;
  openLogin: () => void;
  closeLogin: () => void;
  openCreate: () => void;
  closeCreate: () => void;
}

const AppContext = createContext<AppState | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();

  const [user, setUser] = useState<User | null>(null);
  const [bands, setBands] = useState<Band[]>([]);
  const [currentBandId, setCurrentBandIdState] = useState<string | null>(null);
  const [currentBand, setCurrentBand] = useState<Band | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [invite, setInvite] = useState<InviteInfo | null>(null);
  const [bootLoading, setBootLoading] = useState(true);
  const [bandLoading, setBandLoading] = useState(false);
  const [devRole, setDevRole] = useState<Role | null>(null);

  const [songs, setSongs] = useState<Song[]>(SONGS);
  const [media, setMedia] = useState<MediaItem[]>(MEDIA);

  const [switcherOpen, setSwitcherOpen] = useState(false);
  const [loginOpen, setLoginOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  // 세션 복구: refresh 쿠키로 access 재발급 → 내 정보 + 내 밴드
  useEffect(() => {
    let alive = true;
    (async () => {
      const ok = await authApi.restoreSession();
      if (ok && alive) {
        try {
          const [me, mine] = await Promise.all([authApi.fetchMe(), bandApi.getMyBands()]);
          if (!alive) return;
          setUser(toUser(me));
          setBands(mine.map(toBand));
        } catch {
          // 복구 실패 → 게스트로 진행
        }
      }
      if (alive) setBootLoading(false);
    })();
    return () => {
      alive = false;
    };
  }, []);

  // 현재 밴드 상세 + 멤버 로드
  useEffect(() => {
    if (!currentBandId) {
      setCurrentBand(null);
      setMembers([]);
      return;
    }
    let alive = true;
    setBandLoading(true);
    setInvite(null);
    (async () => {
      try {
        const [band, mem] = await Promise.all([
          bandApi.getBand(currentBandId),
          memberApi.listMembers(currentBandId),
        ]);
        if (!alive) return;
        setCurrentBand(toBand(band));
        setMembers(mem.map((m) => toMember(m, currentBandId)));
      } catch {
        if (alive) {
          setCurrentBand(null);
          setMembers([]);
        }
      } finally {
        if (alive) setBandLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [currentBandId]);

  const setCurrentBandId = useCallback((id: string) => {
    setCurrentBandIdState(id);
    setSwitcherOpen(false);
  }, []);

  // 실제 역할: devRole 우선, 아니면 현재 밴드 멤버 목록에서 내 역할 (없으면 member), 비로그인은 guest
  const role: Role = useMemo(() => {
    if (devRole) return devRole;
    if (!user) return 'guest';
    return members.find((m) => m.id === user.id)?.role ?? 'member';
  }, [devRole, user, members]);

  const login = useCallback(() => {
    authApi.startKakaoLogin();
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setUser(null);
    setBands([]);
    setDevRole(null);
    navigate('/');
  }, [navigate]);

  const createBand = useCallback(
    async (name: string) => {
      const trimmed = name.trim();
      if (!trimmed) return;
      const dto = await bandApi.createBand(trimmed);
      const band = toBand({ ...dto, role: 'OWNER' });
      setBands((prev) => [...prev, band]);
      setCreateOpen(false);
      setSwitcherOpen(false);
      navigate(`/bands/${band.id}`);
    },
    [navigate],
  );

  const joinByInvite = useCallback(async (code: string) => {
    const dto = await inviteApi.joinByCode(code);
    const band = toBand({ ...dto, role: 'MEMBER' });
    setBands((prev) => (prev.some((b) => b.id === band.id) ? prev : [...prev, band]));
    return band;
  }, []);

  const kickMember = useCallback(
    async (userId: string) => {
      if (!currentBandId) return;
      await memberApi.kickMember(currentBandId, userId);
      setMembers((prev) => prev.filter((m) => m.id !== userId));
      setCurrentBand((prev) =>
        prev ? { ...prev, memberCount: Math.max(1, prev.memberCount - 1) } : prev,
      );
      setBands((prev) =>
        prev.map((b) =>
          b.id === currentBandId ? { ...b, memberCount: Math.max(1, b.memberCount - 1) } : b,
        ),
      );
    },
    [currentBandId],
  );

  const issueInviteCode = useCallback(async () => {
    if (!currentBandId) return;
    const dto = await inviteApi.issueInviteCode(currentBandId);
    setInvite({ code: dto.code, url: dto.inviteUrl });
  }, [currentBandId]);

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

  const value: AppState = {
    user,
    bands,
    currentBandId,
    currentBand,
    bootLoading,
    bandLoading,
    role,
    devRole,
    songs,
    media,
    members,
    invite,
    switcherOpen,
    loginOpen,
    createOpen,
    setCurrentBandId,
    setDevRole,
    login,
    logout,
    createBand,
    joinByInvite,
    voteSong,
    promoteSong,
    assignPart,
    addSong,
    addMedia,
    kickMember,
    issueInviteCode,
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
