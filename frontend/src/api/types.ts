// 백엔드(Spring) 응답 DTO 형태. 프론트 도메인 타입(../types)과는 ./mappers 로 이어붙인다.

export interface ErrorBody {
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp: string;
}

/** 백엔드 공통 에러 스키마(ErrorResponse)를 감싼 예외. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(body: ErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = body.status;
    this.code = body.code;
  }
}

export type BandRoleDto = 'OWNER' | 'MEMBER';

export interface BandDto {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  memberCount: number;
  /** GET /api/bands/my 에서만 채워질 예정. 없으면 매퍼가 'member' 로 fallback. */
  role?: BandRoleDto;
  createdAt: string;
}

export interface MemberDto {
  userId: number;
  nickname: string;
  role: BandRoleDto;
  joinedAt: string;
}

export interface InviteCodeDto {
  code: string;
  inviteUrl: string;
  expiresAt: string | null;
  maxUses: number | null;
  usedCount: number;
}

export interface MeDto {
  id: number;
  nickname: string;
}

export interface AccessTokenDto {
  accessToken: string;
  expiresIn: number;
}

/* ── 곡 (Song) ───────────────────────────────────────────────── */

export type SongStatusDto = 'WISHLIST' | 'CONFIRMED';
export type SongSourceTypeDto = 'SEARCH' | 'MANUAL';

export interface SongPartDto {
  id: number;
  instrument: string;
  partIndex: number;
  assignedUserId: number | null;
  assignedNickname: string | null;
}

export interface SongDto {
  id: number;
  bandId: number;
  title: string;
  artist: string | null;
  status: SongStatusDto;
  sourceType: SongSourceTypeDto;
  externalTrackId: string | null;
  memo: string | null;
  referenceVideoUrl: string | null;
  addedByUserId: number;
  addedByNickname: string;
  voteCount: number;
  votedByMe: boolean;
  parts: SongPartDto[];
  createdAt: string;
}

/** POST/DELETE /api/songs/{id}/vote 응답 — 버튼만 갱신하면 되게. */
export interface VoteResultDto {
  voteCount: number;
  votedByMe: boolean;
}

export interface TrackSearchResultDto {
  externalTrackId: string;
  title: string;
  artist: string;
}

export interface SongCreateBody {
  title: string;
  artist: string;
  sourceType: SongSourceTypeDto;
  externalTrackId?: string | null;
  memo?: string;
  referenceVideoUrl?: string;
  /** 악기별 필요 인원 → SongPart 슬롯 */
  sessions: { instrument: string; count: number }[];
}

/* ── 일정 (Schedule) ─────────────────────────────────────────── */

export type ScheduleTypeDto = 'REHEARSAL' | 'PERFORMANCE';
export type AttendanceStatusDto = 'ATTENDING' | 'UNDECIDED' | 'ABSENT';

export interface AttendeeDto {
  userId: number;
  nickname: string;
  status: AttendanceStatusDto;
}

export interface ScheduleDto {
  id: number;
  bandId: number;
  type: ScheduleTypeDto;
  /** ISO-8601 (Instant) */
  dateTime: string;
  location: string | null;
  createdByUserId: number;
  createdByNickname: string;
  counts: { attending: number; absent: number; undecided: number };
  /** 비회원/미응답이면 null */
  myStatus: AttendanceStatusDto | null;
  attendees: AttendeeDto[];
  media: MediaDto[];
  createdAt: string;
}

/* ── 영상 (Media) ────────────────────────────────────────────── */

export type MediaTypeDto = 'REHEARSAL' | 'PERFORMANCE';
export type MediaVisibilityDto = 'MEMBERS_ONLY' | 'LINK_PUBLIC';
export type MediaPlatformDto = 'YOUTUBE' | 'GOOGLE_DRIVE' | 'OTHER';

export interface MediaDto {
  id: number;
  bandId: number;
  scheduleId: number | null;
  type: MediaTypeDto;
  externalUrl: string;
  platform: MediaPlatformDto;
  visibility: MediaVisibilityDto;
  uploadedByUserId: number;
  uploadedByNickname: string;
  createdAt: string;
}

export interface MediaCreateBody {
  externalUrl: string;
  type: MediaTypeDto;
  visibility?: MediaVisibilityDto;
  scheduleId?: number | null;
}
