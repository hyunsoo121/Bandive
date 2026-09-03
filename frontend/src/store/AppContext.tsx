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
  AttendanceStatus,
  Band,
  MediaItem,
  MediaKind,
  Member,
  Role,
  ScheduleEvent,
  ScheduleType,
  SessionShape,
  Song,
  SourceType,
  User,
  Visibility,
} from '../types';
import * as authApi from '../api/auth';
import * as bandApi from '../api/bands';
import * as inviteApi from '../api/invites';
import * as memberApi from '../api/members';
import * as songApi from '../api/songs';
import * as scheduleApi from '../api/schedules';
import * as mediaApi from '../api/media';
import { toBand, toMedia, toMember, toSchedule, toSong, toUser } from '../api/mappers';
import { ATT_TO_EN, byDateAsc } from '../lib/schedule';

export interface NewSongInput {
  bandId: string;
  title: string;
  artist: string;
  sourceType: SourceType;
  /** SEARCH 일 때 검색 결과의 트랙 식별자 (백엔드 필수) */
  externalTrackId?: string | null;
  memo: string;
  referenceVideoUrl: string;
  sessions: SessionShape;
}

export interface NewMediaInput {
  bandId: string;
  url: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결할 일정 id. 없으면 null */
  scheduleId: string | null;
}

export interface NewScheduleInput {
  bandId: string;
  type: ScheduleType;
  /** ISO-8601 (Instant) */
  dateTime: string;
  location: string;
}

/** 현재 밴드의 초대 코드 (밴드장이 발급/재발급한 뒤에만 채워진다 — 조회 전용 API 가 없어서). */
export interface InviteInfo {
  code: string;
  url: string;
}

/**
 * 앱 전역 상태. 인증(카카오 OAuth) · 밴드 · 멤버 · 초대 · 곡 · 일정 · 영상 모두 백엔드 API 연동됨.
 * 현재 밴드가 바뀌면 곡/일정/영상을 그 밴드 것으로 다시 불러온다.
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
  /** 현재 밴드 상세/멤버/콘텐츠 로딩 중 */
  bandLoading: boolean;
  /** 화면 권한 판정에 쓰는 최종 역할 (게스트 포함) */
  role: Role;
  /** 개발용 역할 강제 (null 이면 실제 멤버십 기준) */
  devRole: Role | null;

  switcherOpen: boolean;
  loginOpen: boolean;
  createOpen: boolean;

  /** 현재 밴드 곡 (GET /api/bands/{id}/songs) */
  songs: Song[];
  /** 현재 밴드 일정 (GET /api/bands/{id}/schedules), dateTime 오름차순 */
  schedules: ScheduleEvent[];
  /** 현재 밴드 영상 (GET /api/bands/{id}/media) */
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

  /** 투표 토글 (POST/DELETE /api/songs/{id}/vote) */
  voteSong: (songId: string) => Promise<void>;
  /** 위시리스트 → 합주곡 승격 (PATCH /api/songs/{id}/confirm) */
  promoteSong: (songId: string) => Promise<void>;
  /** 파트 슬롯 배정/해제 (PUT /api/songs/{id}/parts/{partId}/assign) */
  assignPart: (songId: string, slotKey: string, memberName: string) => Promise<void>;
  /** 곡 추가 (POST /api/bands/{id}/songs) */
  addSong: (input: NewSongInput) => Promise<void>;
  /** 곡 삭제 (밴드장) */
  removeSong: (songId: string) => Promise<void>;

  /** 일정 등록 (POST /api/bands/{id}/schedules) */
  addSchedule: (input: NewScheduleInput) => Promise<void>;
  /** 일정 삭제 (밴드장) */
  removeSchedule: (scheduleId: string) => Promise<void>;
  /** 내 참석 여부 등록/변경 (POST /api/schedules/{id}/attendance) */
  setAttendance: (scheduleId: string, status: AttendanceStatus) => Promise<void>;

  /** 영상 URL 첨부 (POST /api/bands/{id}/media) */
  addMedia: (input: NewMediaInput) => Promise<void>;
  /** 영상 삭제 (등록자 본인 또는 밴드장) */
  removeMedia: (mediaId: string) => Promise<void>;

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

const sortSchedules = (list: ScheduleEvent[]): ScheduleEvent[] => [...list].sort(byDateAsc);

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

  const [songs, setSongs] = useState<Song[]>([]);
  const [schedules, setSchedules] = useState<ScheduleEvent[]>([]);
  const [media, setMedia] = useState<MediaItem[]>([]);

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

  // 현재 밴드 상세 + 멤버 + 곡/일정/영상 로드
  useEffect(() => {
    if (!currentBandId) {
      setCurrentBand(null);
      setMembers([]);
      setSongs([]);
      setSchedules([]);
      setMedia([]);
      return;
    }
    let alive = true;
    setBandLoading(true);
    setInvite(null);
    (async () => {
      try {
        const [band, mem, songList, schedList, mediaList] = await Promise.all([
          bandApi.getBand(currentBandId),
          memberApi.listMembers(currentBandId),
          songApi.listSongs(currentBandId),
          scheduleApi.listSchedules(currentBandId),
          mediaApi.listMedia(currentBandId),
        ]);
        if (!alive) return;
        setCurrentBand(toBand(band));
        setMembers(mem.map((m) => toMember(m, currentBandId)));
        setSongs(songList.map(toSong));
        setSchedules(sortSchedules(schedList.map(toSchedule)));
        setMedia(mediaList.map(toMedia));
      } catch {
        if (alive) {
          setCurrentBand(null);
          setMembers([]);
          setSongs([]);
          setSchedules([]);
          setMedia([]);
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

  const voteSong = useCallback(
    async (songId: string) => {
      const song = songs.find((s) => s.id === songId);
      if (!song) return;
      try {
        const result = song.votedByMe
          ? await songApi.unvoteSong(songId)
          : await songApi.voteSong(songId);
        setSongs((prev) =>
          prev.map((s) =>
            s.id === songId ? { ...s, votes: result.voteCount, votedByMe: result.votedByMe } : s,
          ),
        );
      } catch (e) {
        console.error('투표 처리 실패', e);
      }
    },
    [songs],
  );

  const promoteSong = useCallback(async (songId: string) => {
    try {
      const dto = await songApi.confirmSong(songId);
      setSongs((prev) => prev.map((s) => (s.id === songId ? toSong(dto) : s)));
    } catch (e) {
      console.error('합주곡 승격 실패', e);
    }
  }, []);

  const assignPart = useCallback(
    async (songId: string, slotKey: string, memberName: string) => {
      const song = songs.find((s) => s.id === songId);
      if (!song) return;
      const hashAt = slotKey.lastIndexOf('#');
      const instrument = slotKey.slice(0, hashAt);
      const partIndex = Number(slotKey.slice(hashAt + 1));
      const part = song.parts.find((p) => p.instrument === instrument && p.partIndex === partIndex);
      if (!part) return;
      const userId = memberName ? (members.find((m) => m.name === memberName)?.id ?? null) : null;
      try {
        const dto = await songApi.assignPart(songId, part.id, userId);
        setSongs((prev) => prev.map((s) => (s.id === songId ? toSong(dto) : s)));
      } catch (e) {
        console.error('파트 배정 실패', e);
      }
    },
    [songs, members],
  );

  const addSong = useCallback(async (input: NewSongInput) => {
    const sessions = Object.entries(input.sessions)
      .filter(([, count]) => (count ?? 0) > 0)
      .map(([instrument, count]) => ({ instrument, count: count as number }));
    const dto = await songApi.addSong(input.bandId, {
      title: input.title.trim(),
      artist: input.artist.trim(),
      sourceType: input.sourceType,
      externalTrackId: input.externalTrackId ?? null,
      memo: input.memo.trim(),
      referenceVideoUrl: input.referenceVideoUrl.trim(),
      sessions,
    });
    setSongs((prev) => [...prev, toSong(dto)]);
  }, []);

  const removeSong = useCallback(async (songId: string) => {
    await songApi.deleteSong(songId);
    setSongs((prev) => prev.filter((s) => s.id !== songId));
  }, []);

  const addSchedule = useCallback(async (input: NewScheduleInput) => {
    const dto = await scheduleApi.createSchedule(input.bandId, {
      type: input.type,
      dateTime: input.dateTime,
      location: input.location.trim() || undefined,
    });
    setSchedules((prev) => sortSchedules([...prev, toSchedule(dto)]));
  }, []);

  const removeSchedule = useCallback(async (scheduleId: string) => {
    await scheduleApi.deleteSchedule(scheduleId);
    setSchedules((prev) => prev.filter((s) => s.id !== scheduleId));
  }, []);

  const setAttendance = useCallback(async (scheduleId: string, status: AttendanceStatus) => {
    try {
      const dto = await scheduleApi.setAttendance(scheduleId, ATT_TO_EN[status]);
      setSchedules((prev) => prev.map((s) => (s.id === scheduleId ? toSchedule(dto) : s)));
    } catch (e) {
      console.error('출결 저장 실패', e);
    }
  }, []);

  // 일정에 연결된 영상이 바뀌면 그 일정의 mediaIds 도 다시 받아야 상세에 반영된다.
  const refreshSchedules = useCallback(async (bandId: string) => {
    const list = await scheduleApi.listSchedules(bandId);
    setSchedules(sortSchedules(list.map(toSchedule)));
  }, []);

  const addMedia = useCallback(
    async (input: NewMediaInput) => {
      const dto = await mediaApi.addMedia(input.bandId, {
        externalUrl: input.url.trim(),
        type: input.kind === '공연' ? 'PERFORMANCE' : 'REHEARSAL',
        visibility: input.visibility === '링크 공개' ? 'LINK_PUBLIC' : 'MEMBERS_ONLY',
        scheduleId: input.scheduleId ? Number(input.scheduleId) : null,
      });
      setMedia((prev) => [toMedia(dto), ...prev]);
      if (input.scheduleId) await refreshSchedules(input.bandId);
    },
    [refreshSchedules],
  );

  const removeMedia = useCallback(
    async (mediaId: string) => {
      const linkedScheduleId = media.find((m) => m.id === mediaId)?.scheduleId ?? null;
      await mediaApi.deleteMedia(mediaId);
      setMedia((prev) => prev.filter((m) => m.id !== mediaId));
      if (linkedScheduleId && currentBandId) await refreshSchedules(currentBandId);
    },
    [media, currentBandId, refreshSchedules],
  );

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
    schedules,
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
    removeSong,
    addSchedule,
    removeSchedule,
    setAttendance,
    addMedia,
    removeMedia,
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
