// 도메인 타입. band_archive_plan.md 의 ERD를 프론트 관점으로 정리.
// 백엔드 붙기 전까지 mock 데이터가 이 형태를 따른다.

export type Role = 'owner' | 'member' | 'guest';

/** 자주 쓰는 악기 (기본 세션 구성). 이 외에 자유 문자열도 허용된다. */
export type Instrument = '보컬' | '기타' | '베이스' | '드럼' | '건반';
export const INSTRUMENTS: Instrument[] = ['보컬', '기타', '베이스', '드럼', '건반'];

export type SongStatus = 'WISHLIST' | 'CONFIRMED';
export type SourceType = 'SEARCH' | 'MANUAL';

export type ScheduleType = 'REHEARSAL' | 'PERFORMANCE';
export type AttendanceStatus = '참석' | '미정' | '불참';

export type MediaKind = '합주' | '공연';
export type Visibility = '멤버만' | '링크 공개';

export interface User {
  id: string;
  name: string;
  initial: string;
}

export interface SongFolder {
  id: string;
  name: string;
  /** 이 폴더가 위시리스트/합주곡 중 어디에 속하는지 */
  status: 'WISHLIST' | 'CONFIRMED';
  position: number;
}

export interface Band {
  id: string;
  name: string;
  initial: string;
  memberCount: number;
  myRole: Role;
  /** 홈 배너/서브텍스트용 요약 */
  note: string;
  /** 밴드 로고 (절대 URL). 없으면 null → 이니셜 아바타 */
  logoUrl: string | null;
  /** 밴드 배너 (절대 URL). 없으면 null → 줄무늬 플레이스홀더 */
  bannerUrl: string | null;
}

export interface Member {
  id: string;
  bandId: string;
  name: string;
  initial: string;
  part: string;
  role: 'owner' | 'member';
  avatarColor: string;
}

/** 악기 이름 → 필요 인원. 기본 악기 + 자유 문자열 악기(실로폰 등). */
export type SessionShape = Record<string, number>;

/** 곡의 파트 슬롯 하나. 백엔드 SongPart 를 화면에서 쓰기 좋게 줄인 것. */
export interface SongPartLite {
  id: string;
  instrument: string;
  partIndex: number;
  /** 배정된 멤버의 userId. 없으면 null */
  assigneeId: string | null;
  assigneeName: string | null;
}

export interface Song {
  id: string;
  bandId: string;
  title: string;
  artist: string;
  status: SongStatus;
  sourceType: SourceType;
  proposer: string;
  memo: string;
  referenceVideoUrl: string;
  /** 앨범 커버 이미지 URL (SEARCH 로 추가 시). 없으면 null */
  artworkUrl: string | null;
  sessions: SessionShape;
  votes: number;
  votedByMe: boolean;
  /** 정렬용 — 값이 클수록 최근 (createdAt epoch ms) */
  addedOrder: number;
  /** 슬롯키("기타#2") -> 멤버 이름. CONFIRMED 에서만 채운다 */
  assignments: Record<string, string>;
  /** 속한 폴더 id. null = 미분류 */
  folderId: string | null;
  /** 그룹(status × 폴더/미분류) 안 수동 정렬 위치. 0 부터 */
  position: number;
  /** 원본 파트 슬롯 — 배정 API(partId 필요) 호출용 */
  parts: SongPartLite[];
}

export interface ScheduleAttendee {
  userId: string;
  nickname: string;
  status: AttendanceStatus;
}

export interface ScheduleEvent {
  id: string;
  bandId: string;
  type: ScheduleType;
  /** ISO-8601 (Instant) */
  dateTime: string;
  location: string;
  counts: { attending: number; absent: number; undecided: number };
  /** 내 참석 여부. 비회원/미응답이면 null */
  myStatus: AttendanceStatus | null;
  /** 응답한 멤버만 */
  attendees: ScheduleAttendee[];
  /** 이 일정에 연결된 영상 id */
  mediaIds: string[];
}

export interface MediaItem {
  id: string;
  bandId: string;
  /** 외부 영상 URL (유튜브/구글드라이브 등) */
  url: string;
  /** 표시용 제목. 실제 제목이 없으면 URL 파생 라벨 */
  title: string;
  /** 백엔드에 저장된 실제 제목 (수정 폼 프리필용). 없으면 null */
  rawTitle: string | null;
  /** 계산된 썸네일 이미지 URL. 없으면 null → 플랫폼 아이콘 */
  thumbnailUrl: string | null;
  source: string;
  /** 'YouTube' | 'Google Drive' | '링크' — 아이콘/경고 판단용 */
  platform: 'youtube' | 'drive' | 'other';
  date: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 등록자 userId (수정·삭제 권한 판단) */
  uploadedByUserId: string;
  /** 연결된 일정 id. 없으면 null */
  scheduleId: string | null;
}
