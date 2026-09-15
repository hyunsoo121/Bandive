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

export type BandVisibilityDto = 'PRIVATE' | 'FOLLOWERS' | 'PUBLIC';
export type BandRelationDto = 'MEMBER' | 'FOLLOWER' | 'PENDING' | 'NONE';
export type FollowStatusDto = 'PENDING' | 'APPROVED';

/* ── 탐색 (Explore) ──────────────────────────────────────────── */

export interface ExploreBandDto {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  visibility: BandVisibilityDto;
  memberCount: number;
}

export interface ExploreTrackDto {
  externalTrackId: string;
  title: string;
  artist: string | null;
  artworkUrl: string | null;
  bandCount: number;
  videoCount: number;
}

export interface ExploreVideoDto {
  mediaId: number;
  bandId: number;
  bandName: string;
  url: string;
  title: string | null;
  thumbnailUrl: string | null;
  platform: MediaPlatformDto;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface ExploreBandDetailDto {
  band: ExploreBandDto;
  myRelation: BandRelationDto;
  /** PUBLIC 밴드면 공개 합주 영상, FOLLOWERS 면 빈 배열 */
  videos: ExploreVideoDto[];
}

export interface FollowerDto {
  userId: number;
  nickname: string;
  status: FollowStatusDto;
  requestedAt: string;
  decidedAt: string | null;
}

/** GET /api/bands/{bandId}/followers/public — 승인된 팔로워만, 콘텐츠 열람 가능한 사람이면 누구나. */
export interface PublicFollowerDto {
  userId: number;
  nickname: string;
  avatarUrl: string | null;
}

/** GET /api/me/following — 내가 팔로우한 밴드 한 곳 */
export interface FollowingBandDto {
  bandId: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  visibility: BandVisibilityDto;
  memberCount: number;
  status: FollowStatusDto;
  requestedAt: string;
}

export interface BandDto {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  memberCount: number;
  /** 승인된 팔로워 수. FOLLOWERS 밴드가 아니면 0 */
  followerCount: number;
  visibility: BandVisibilityDto;
  /** 현재 사용자와의 관계. 항상 채워짐 (비로그인이면 NONE). */
  myRelation: BandRelationDto;
  /** GET /api/bands/my 에서만 채워질 예정. 없으면 매퍼가 'member' 로 fallback. */
  role?: BandRoleDto;
  createdAt: string;
}

export interface MemberDto {
  userId: number;
  nickname: string;
  /** 프로필 사진 URL. 없으면 null */
  avatarUrl: string | null;
  role: BandRoleDto;
  leader: boolean;
  parts: string[];
  joinedAt: string;
}

export interface InviteCodeDto {
  code: string;
  inviteUrl: string;
  expiresAt: string | null;
  maxUses: number | null;
  usedCount: number;
}

/** GET /api/invite-codes/{code} — 가입 전 미리보기(공개, 비로그인도 조회 가능). */
export interface InvitePreviewDto {
  code: string;
  bandId: number;
  bandName: string;
  description: string | null;
  logoUrl: string | null;
  memberCount: number;
}

export interface MeDto {
  id: number;
  nickname: string;
  /** LOCAL 가입자만 값이 있음. 카카오 가입자는 null */
  email: string | null;
  /** 프로필 사진 URL. 없으면 null */
  avatarUrl: string | null;
  /** 한 줄 소개. 없으면 null */
  bio: string | null;
  provider: 'KAKAO' | 'LOCAL';
}

/** GET /api/users/{id} — 다른 사람 공개 프로필 */
export interface UserProfileDto {
  id: number;
  nickname: string;
  avatarUrl: string | null;
  bio: string | null;
  bands: {
    id: number;
    name: string;
    logoUrl: string | null;
    memberCount: number;
    role: BandRoleDto;
  }[];
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
  /** 배정된 실멤버 userId. 게스트 배정이면 null */
  assignedUserId: number | null;
  /** 배정된 게스트 id. 실멤버 배정이면 null */
  assignedGuestId: number | null;
  /** 배정된 쪽(멤버 or 게스트)의 표시 이름 */
  assignedName: string | null;
}

/** 파트 배정 바디 — userId / guestId 중 최대 하나. 둘 다 null 이면 해제. */
export interface PartAssignBody {
  userId: number | null;
  guestId: number | null;
}

export interface SongDto {
  id: number;
  bandId: number;
  title: string;
  artist: string | null;
  status: SongStatusDto;
  sourceType: SongSourceTypeDto;
  externalTrackId: string | null;
  artworkUrl: string | null;
  memo: string | null;
  referenceVideoUrl: string | null;
  addedByUserId: number;
  addedByNickname: string;
  voteCount: number;
  votedByMe: boolean;
  folderId: number | null;
  position: number;
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
  /** 앨범 커버 URL. 없으면 null */
  artworkUrl: string | null;
}

export interface SongCreateBody {
  title: string;
  artist: string;
  sourceType: SongSourceTypeDto;
  externalTrackId?: string | null;
  artworkUrl?: string | null;
  memo?: string;
  referenceVideoUrl?: string;
  /** 악기별 필요 인원 → SongPart 슬롯 */
  sessions: { instrument: string; count: number }[];
}

/* ── 일정 (Schedule) ─────────────────────────────────────────── */

export type ScheduleTypeDto = 'REHEARSAL' | 'PERFORMANCE';
export type AttendanceStatusDto = 'ATTENDING' | 'UNDECIDED' | 'ABSENT';

export interface AttendeeDto {
  /** 실멤버 출결이면 userId, 게스트 참석이면 null */
  userId: number | null;
  /** 게스트 참석이면 guestId, 실멤버 출결이면 null */
  guestId: number | null;
  nickname: string;
  status: AttendanceStatusDto;
}

/* ── 게스트 멤버 (Guest) ─────────────────────────────────────── */

export interface GuestDto {
  id: number;
  bandId: number;
  name: string;
  /** 이 밴드에서 맡는 세션(악기 또는 "관객"). 없으면 null */
  session: string | null;
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
  /** 연결된 합주곡 id. 없으면 null */
  songId: number | null;
  /** 연결된 곡 제목 (표시용). 없으면 null */
  songTitle: string | null;
  type: MediaTypeDto;
  externalUrl: string;
  /** 사용자가 붙인 제목. 없으면 null */
  title: string | null;
  platform: MediaPlatformDto;
  /** 계산된 썸네일 URL (YouTube/Drive). 없으면 null */
  thumbnailUrl: string | null;
  visibility: MediaVisibilityDto;
  uploadedByUserId: number;
  uploadedByNickname: string;
  /** 좋아요 수 */
  likeCount: number;
  /** 현재 로그인 유저가 좋아요 눌렀는지. 비회원이면 항상 false */
  likedByMe: boolean;
  createdAt: string;
}

/** POST/DELETE /api/media/{id}/like 응답 — 버튼만 갱신하면 되게. */
export interface MediaLikeResultDto {
  likeCount: number;
  likedByMe: boolean;
}

export interface MediaCreateBody {
  externalUrl: string;
  type: MediaTypeDto;
  title?: string;
  visibility?: MediaVisibilityDto;
  scheduleId?: number | null;
  songId?: number | null;
}

/** 부분 수정 — 보낸 필드만 반영. */
export interface MediaUpdateBody {
  externalUrl?: string;
  type?: MediaTypeDto;
  title?: string;
  visibility?: MediaVisibilityDto;
  scheduleId?: number | null;
  songId?: number | null;
}
