// 도메인 타입. band_archive_plan.md 의 ERD를 프론트 관점으로 정리.
// 백엔드 붙기 전까지 mock 데이터가 이 형태를 따른다.

export type Role = 'owner' | 'member' | 'guest';

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

export interface Band {
  id: string;
  name: string;
  initial: string;
  memberCount: number;
  myRole: Role;
  /** 홈 배너/서브텍스트용 요약 */
  note: string;
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

/** 악기별 필요 인원 (세션 구성) */
export type SessionShape = Partial<Record<Instrument, number>>;

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
  sessions: SessionShape;
  votes: number;
  votedByMe: boolean;
  /** 정렬용 — 값이 클수록 최근 (createdAt epoch ms) */
  addedOrder: number;
  /** 슬롯키("기타#2") -> 멤버 이름. CONFIRMED 에서만 채운다 */
  assignments: Record<string, string>;
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
  /** 표시용 라벨 (백엔드에 제목 필드가 없어 URL 에서 파생) */
  title: string;
  source: string;
  date: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결된 일정 id. 없으면 null */
  scheduleId: string | null;
}
