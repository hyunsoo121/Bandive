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
  SongFolder,
  SourceType,
  User,
  Visibility,
} from '../types';
import * as authApi from '../api/auth';
import * as bandApi from '../api/bands';
import * as inviteApi from '../api/invites';
import * as memberApi from '../api/members';
import * as songApi from '../api/songs';
import * as songFolderApi from '../api/songFolders';
import * as scheduleApi from '../api/schedules';
import * as mediaApi from '../api/media';
import {
  toBand,
  toMedia,
  toMember,
  toSchedule,
  toSong,
  toSongFolder,
  toUser,
} from '../api/mappers';
import { ATT_TO_EN, byDateAsc } from '../lib/schedule';

export interface NewSongInput {
  bandId: string;
  title: string;
  artist: string;
  sourceType: SourceType;
  /** SEARCH 일 때 검색 결과의 트랙 식별자 (백엔드 필수) */
  externalTrackId?: string | null;
  /** SEARCH 일 때 앨범 커버 URL */
  artworkUrl?: string | null;
  memo: string;
  referenceVideoUrl: string;
  sessions: SessionShape;
}

export interface NewMediaInput {
  bandId: string;
  url: string;
  /** 사용자가 붙인 제목 (선택) */
  title: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결할 일정 id. 없으면 null */
  scheduleId: string | null;
}

export interface EditMediaInput {
  url: string;
  title: string;
  kind: MediaKind;
  visibility: Visibility;
  scheduleId: string | null;
}

export interface NewScheduleInput {
  bandId: string;
  type: ScheduleType;
  /** ISO-8601 (Instant) */
  dateTime: string;
  location: string;
}

/** 현재 밴드의 초대 코드 (관리자가 발급/재발급한 뒤에만 채워진다 — 조회 전용 API 가 없어서). */
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
  profileOpen: boolean;

  /** 현재 밴드 곡 (GET /api/bands/{id}/songs) */
  songs: Song[];
  /** 현재 밴드 곡 폴더 (GET /api/bands/{id}/song-folders), status·position 순 */
  songFolders: SongFolder[];
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
  /** 이메일 로그인 — 성공 시 세션 반영 + 로그인 모달 닫힘 */
  emailLogin: (email: string, password: string) => Promise<void>;
  /** 이메일 회원가입 — 성공 시 바로 로그인 상태 */
  signup: (email: string, password: string, nickname: string) => Promise<void>;
  logout: () => void;
  /** 내 닉네임 수정 (PATCH /api/auth/me) */
  updateProfile: (nickname: string) => Promise<void>;
  /** 비밀번호 변경 (이메일 로그인 계정만) */
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  /** 밴드 생성 → 내 밴드에 추가하고 해당 밴드로 이동 */
  createBand: (name: string) => Promise<void>;
  /** 초대 코드로 가입 → 가입한 밴드 반환 */
  joinByInvite: (code: string) => Promise<Band>;
  /** 밴드 이름·소개 수정 (관리자) */
  updateBand: (name: string, description: string | null) => Promise<void>;
  /** 관리자 위임 (관리자) */
  transferOwnership: (userId: string) => Promise<void>;
  /** 밴드 삭제 (관리자) → 홈으로 */
  deleteBand: () => Promise<void>;
  /** 밴드 탈퇴 (일반 멤버) → 홈으로. 관리자는 불가(위임/삭제 먼저) */
  leaveBand: () => Promise<void>;
  /** 밴드 로고 이미지 업로드 (관리자) */
  uploadBandLogo: (file: File) => Promise<void>;
  /** 밴드 배너 이미지 업로드 (관리자) */
  uploadBandBanner: (file: File) => Promise<void>;

  /** 투표 토글 (POST/DELETE /api/songs/{id}/vote) */
  voteSong: (songId: string) => Promise<void>;
  /** 위시리스트 → 합주곡 승격 (PATCH /api/songs/{id}/confirm) */
  promoteSong: (songId: string) => Promise<void>;
  /** 파트 슬롯 배정/해제 (PUT /api/songs/{id}/parts/{partId}/assign) */
  assignPart: (songId: string, slotKey: string, memberName: string) => Promise<void>;
  /** 곡 추가 (POST /api/bands/{id}/songs) */
  addSong: (input: NewSongInput) => Promise<void>;
  /** 곡 삭제 (관리자) */
  removeSong: (songId: string) => Promise<void>;
  /** 곡을 폴더로 이동 (멤버 누구나). folderId null = 미분류. 대상 그룹 맨 끝으로 */
  moveSongToFolder: (songId: string, folderId: string | null) => Promise<void>;
  /** 한 그룹(status × 폴더/미분류) 안 곡 순서 재지정 (멤버 누구나) */
  reorderSongs: (
    status: Song['status'],
    folderId: string | null,
    songIds: string[],
  ) => Promise<void>;
  /** 곡 폴더 생성 (관리자) */
  createSongFolder: (name: string, status: Song['status']) => Promise<void>;
  /** 곡 폴더 이름 변경 (관리자) */
  renameSongFolder: (folderId: string, name: string) => Promise<void>;
  /** 곡 폴더 삭제 (관리자) — 소속 곡은 미분류로 */
  removeSongFolder: (folderId: string) => Promise<void>;
  /** 한 status 안에서 폴더 순서 재지정 (관리자) */
  reorderSongFolders: (status: Song['status'], folderIds: string[]) => Promise<void>;

  /** 일정 등록 (POST /api/bands/{id}/schedules) */
  addSchedule: (input: NewScheduleInput) => Promise<void>;
  /** 일정 삭제 (관리자) */
  removeSchedule: (scheduleId: string) => Promise<void>;
  /** 내 참석 여부 등록/변경 (POST /api/schedules/{id}/attendance) */
  setAttendance: (scheduleId: string, status: AttendanceStatus) => Promise<void>;

  /** 영상 URL 첨부 (POST /api/bands/{id}/media) */
  addMedia: (input: NewMediaInput) => Promise<void>;
  /** 영상 수정 (PATCH /api/media/{id}) — 등록자 본인 또는 관리자 */
  editMedia: (mediaId: string, input: EditMediaInput) => Promise<void>;
  /** 영상 삭제 (등록자 본인 또는 관리자) */
  removeMedia: (mediaId: string) => Promise<void>;

  /** 멤버 추방 (관리자) — userId */
  kickMember: (userId: string) => Promise<void>;
  /** 멤버 세션(파트) 전체 교체. 본인 또는 관리자 */
  setMemberParts: (userId: string, parts: string[]) => Promise<void>;
  /** 밴드 리더 지정/해제 (관리자). null = 리더 없음 */
  setBandLeader: (userId: string | null) => Promise<void>;
  /** 초대 코드 발급/재발급 (관리자) */
  issueInviteCode: () => Promise<void>;

  openSwitcher: () => void;
  closeSwitcher: () => void;
  openLogin: () => void;
  closeLogin: () => void;
  openCreate: () => void;
  closeCreate: () => void;
  openProfile: () => void;
  closeProfile: () => void;
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
  const [songFolders, setSongFolders] = useState<SongFolder[]>([]);
  const [schedules, setSchedules] = useState<ScheduleEvent[]>([]);
  const [media, setMedia] = useState<MediaItem[]>([]);

  const [switcherOpen, setSwitcherOpen] = useState(false);
  const [loginOpen, setLoginOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

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
      setSongFolders([]);
      setSchedules([]);
      setMedia([]);
      return;
    }
    let alive = true;
    setBandLoading(true);
    setInvite(null);
    (async () => {
      try {
        const [band, mem, songList, folderList, schedList, mediaList] = await Promise.all([
          bandApi.getBand(currentBandId),
          memberApi.listMembers(currentBandId),
          songApi.listSongs(currentBandId),
          songFolderApi.listFolders(currentBandId),
          scheduleApi.listSchedules(currentBandId),
          mediaApi.listMedia(currentBandId),
        ]);
        if (!alive) return;
        setCurrentBand(toBand(band));
        setMembers(mem.map((m) => toMember(m, currentBandId)));
        setSongs(songList.map(toSong));
        setSongFolders(folderList.map(toSongFolder));
        setSchedules(sortSchedules(schedList.map(toSchedule)));
        setMedia(mediaList.map(toMedia));
      } catch {
        if (alive) {
          setCurrentBand(null);
          setMembers([]);
          setSongs([]);
          setSongFolders([]);
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

  // 이메일 로그인/가입 성공 후 공통: 내 정보 + 내 밴드를 불러와 세션에 반영하고 모달을 닫는다.
  const finishAuth = useCallback(async () => {
    const [me, mine] = await Promise.all([authApi.fetchMe(), bandApi.getMyBands()]);
    setUser(toUser(me));
    setBands(mine.map(toBand));
    setDevRole(null);
    setLoginOpen(false);
  }, []);

  const emailLogin = useCallback(
    async (email: string, password: string) => {
      await authApi.loginWithEmail(email.trim(), password);
      await finishAuth();
    },
    [finishAuth],
  );

  const signup = useCallback(
    async (email: string, password: string, nickname: string) => {
      await authApi.signupWithEmail(email.trim(), password, nickname.trim());
      await finishAuth();
    },
    [finishAuth],
  );

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
      const band = toBand(dto); // 생성 응답에 role: 'OWNER' 포함됨
      setBands((prev) => [...prev, band]);
      setCreateOpen(false);
      setSwitcherOpen(false);
      navigate(`/bands/${band.id}`);
    },
    [navigate],
  );

  const joinByInvite = useCallback(async (code: string) => {
    const dto = await inviteApi.joinByCode(code);
    const band = toBand(dto); // 가입 응답에 role: 'MEMBER' 포함됨
    setBands((prev) => (prev.some((b) => b.id === band.id) ? prev : [...prev, band]));
    return band;
  }, []);

  const applyBandUpdate = useCallback((band: Band) => {
    setCurrentBand(band);
    setBands((prev) => prev.map((b) => (b.id === band.id ? band : b)));
  }, []);

  const uploadBandLogo = useCallback(
    async (file: File) => {
      if (!currentBandId) return;
      applyBandUpdate(toBand(await bandApi.uploadLogo(currentBandId, file)));
    },
    [currentBandId, applyBandUpdate],
  );

  const uploadBandBanner = useCallback(
    async (file: File) => {
      if (!currentBandId) return;
      applyBandUpdate(toBand(await bandApi.uploadBanner(currentBandId, file)));
    },
    [currentBandId, applyBandUpdate],
  );

  const updateBand = useCallback(
    async (name: string, description: string | null) => {
      if (!currentBandId) return;
      applyBandUpdate(toBand(await bandApi.updateBand(currentBandId, name.trim(), description)));
    },
    [currentBandId, applyBandUpdate],
  );

  const transferOwnership = useCallback(
    async (userId: string) => {
      if (!currentBandId) return;
      await memberApi.transferOwnership(currentBandId, userId);
      // 역할이 바뀌었으니 밴드/멤버를 다시 받아 반영
      const [bandDto, memberDtos] = await Promise.all([
        bandApi.getBand(currentBandId),
        memberApi.listMembers(currentBandId),
      ]);
      applyBandUpdate(toBand(bandDto));
      setMembers(memberDtos.map((m) => toMember(m, currentBandId)));
    },
    [currentBandId, applyBandUpdate],
  );

  const deleteBand = useCallback(async () => {
    if (!currentBandId) return;
    const gone = currentBandId;
    await memberApi.deleteBand(gone);
    setBands((prev) => prev.filter((b) => b.id !== gone));
    setCurrentBandIdState(null);
    navigate('/');
  }, [currentBandId, navigate]);

  const leaveBand = useCallback(async () => {
    if (!currentBandId) return;
    const gone = currentBandId;
    await memberApi.leaveBand(gone);
    setBands((prev) => prev.filter((b) => b.id !== gone));
    setCurrentBandIdState(null);
    navigate('/');
  }, [currentBandId, navigate]);

  const updateProfile = useCallback(async (nickname: string) => {
    setUser(toUser(await authApi.updateMe(nickname.trim())));
  }, []);

  const changePassword = useCallback(
    (currentPassword: string, newPassword: string) =>
      authApi.changePassword(currentPassword, newPassword),
    [],
  );

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

  const setMemberParts = useCallback(
    async (userId: string, parts: string[]) => {
      if (!currentBandId) return;
      const dto =
        user && userId === user.id
          ? await memberApi.updateMyParts(currentBandId, parts)
          : await memberApi.updateMemberParts(currentBandId, userId, parts);
      const fresh = toMember(dto, currentBandId);
      setMembers((prev) => prev.map((m) => (m.id === userId ? fresh : m)));
    },
    [currentBandId, user],
  );

  const setBandLeader = useCallback(
    async (userId: string | null) => {
      if (!currentBandId) return;
      const dtos = await memberApi.setLeader(currentBandId, userId);
      setMembers(dtos.map((m) => toMember(m, currentBandId)));
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
      artworkUrl: input.artworkUrl ?? null,
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

  const moveSongToFolder = useCallback(async (songId: string, folderId: string | null) => {
    const dto = await songApi.moveSongToFolder(songId, folderId);
    setSongs((prev) => prev.map((s) => (s.id === songId ? toSong(dto) : s)));
  }, []);

  const reorderSongs = useCallback(
    async (status: Song['status'], folderId: string | null, songIds: string[]) => {
      if (!currentBandId) return;
      // 낙관적 반영: 해당 그룹 곡들의 position 을 새 순서 인덱스로
      const order = new Map(songIds.map((id, i) => [id, i]));
      setSongs((prev) =>
        prev.map((s) =>
          s.bandId === currentBandId &&
          s.status === status &&
          (s.folderId ?? null) === folderId &&
          order.has(s.id)
            ? { ...s, position: order.get(s.id) ?? s.position }
            : s,
        ),
      );
      try {
        await songApi.reorderSongs(currentBandId, status, folderId, songIds);
      } catch (e) {
        const fresh = await songApi.listSongs(currentBandId);
        setSongs(fresh.map(toSong));
        throw e;
      }
    },
    [currentBandId],
  );

  const createSongFolder = useCallback(
    async (name: string, status: Song['status']) => {
      if (!currentBandId) return;
      const dto = await songFolderApi.createFolder(currentBandId, name.trim(), status);
      setSongFolders((prev) => [...prev, toSongFolder(dto)]);
    },
    [currentBandId],
  );

  const renameSongFolder = useCallback(async (folderId: string, name: string) => {
    const dto = await songFolderApi.renameFolder(folderId, name.trim());
    setSongFolders((prev) => prev.map((f) => (f.id === folderId ? toSongFolder(dto) : f)));
  }, []);

  const removeSongFolder = useCallback(async (folderId: string) => {
    await songFolderApi.deleteFolder(folderId);
    setSongFolders((prev) => prev.filter((f) => f.id !== folderId));
    setSongs((prev) => prev.map((s) => (s.folderId === folderId ? { ...s, folderId: null } : s)));
  }, []);

  const reorderSongFolders = useCallback(
    async (status: Song['status'], folderIds: string[]) => {
      if (!currentBandId) return;
      // 낙관적 반영 후 서버 응답으로 확정
      setSongFolders((prev) => {
        const order = new Map(folderIds.map((id, i) => [id, i]));
        return prev.map((f) =>
          f.status === status && order.has(f.id)
            ? { ...f, position: order.get(f.id) ?? f.position }
            : f,
        );
      });
      const dtos = await songFolderApi.reorderFolders(currentBandId, status, folderIds);
      const fresh = new Map(dtos.map((d) => [String(d.id), toSongFolder(d)]));
      setSongFolders((prev) => prev.map((f) => fresh.get(f.id) ?? f));
    },
    [currentBandId],
  );

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
        title: input.title.trim() || undefined,
        type: input.kind === '공연' ? 'PERFORMANCE' : 'REHEARSAL',
        visibility: input.visibility === '링크 공개' ? 'LINK_PUBLIC' : 'MEMBERS_ONLY',
        scheduleId: input.scheduleId ? Number(input.scheduleId) : null,
      });
      setMedia((prev) => [toMedia(dto), ...prev]);
      if (input.scheduleId) await refreshSchedules(input.bandId);
    },
    [refreshSchedules],
  );

  const editMedia = useCallback(
    async (mediaId: string, input: EditMediaInput) => {
      const before = media.find((m) => m.id === mediaId)?.scheduleId ?? null;
      const dto = await mediaApi.updateMedia(mediaId, {
        externalUrl: input.url.trim(),
        title: input.title.trim(), // 빈 문자열 → 백엔드에서 제목 제거
        type: input.kind === '공연' ? 'PERFORMANCE' : 'REHEARSAL',
        visibility: input.visibility === '링크 공개' ? 'LINK_PUBLIC' : 'MEMBERS_ONLY',
        scheduleId: input.scheduleId ? Number(input.scheduleId) : null,
      });
      const next = toMedia(dto);
      setMedia((prev) => prev.map((m) => (m.id === mediaId ? next : m)));
      if ((before || next.scheduleId) && currentBandId) await refreshSchedules(currentBandId);
    },
    [media, currentBandId, refreshSchedules],
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
    songFolders,
    schedules,
    media,
    members,
    invite,
    switcherOpen,
    loginOpen,
    createOpen,
    profileOpen,
    setCurrentBandId,
    setDevRole,
    login,
    emailLogin,
    signup,
    logout,
    updateProfile,
    changePassword,
    createBand,
    joinByInvite,
    updateBand,
    transferOwnership,
    deleteBand,
    leaveBand,
    uploadBandLogo,
    uploadBandBanner,
    voteSong,
    promoteSong,
    assignPart,
    addSong,
    removeSong,
    moveSongToFolder,
    reorderSongs,
    createSongFolder,
    renameSongFolder,
    removeSongFolder,
    reorderSongFolders,
    addSchedule,
    removeSchedule,
    setAttendance,
    addMedia,
    editMedia,
    removeMedia,
    kickMember,
    setMemberParts,
    setBandLeader,
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
    openProfile: () => {
      setProfileOpen(true);
      setSwitcherOpen(false);
    },
    closeProfile: () => setProfileOpen(false),
  };

  return <AppContext value={value}>{children}</AppContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useApp(): AppState {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within <AppProvider>');
  return ctx;
}
