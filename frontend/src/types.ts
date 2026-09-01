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
  /** 정렬용 — 값이 클수록 최근 */
  addedOrder: number;
  /** 슬롯키("기타#2") -> 멤버 이름. CONFIRMED 에서만 채운다 */
  assignments: Record<string, string>;
}

export interface ScheduleEvent {
  id: string;
  bandId: string;
  type: ScheduleType;
  /** 2026-08 기준 일(day). 캘린더 목업과 동일하게 단순화 */
  day: number;
  dow: string;
  time: string;
  title: string;
  place: string;
}

export interface MediaItem {
  id: string;
  bandId: string;
  title: string;
  source: 'YouTube' | 'Google Drive';
  date: string;
  kind: MediaKind;
  visibility: Visibility;
  /** 연결된 일정의 day. 없으면 null */
  scheduleDay: number | null;
}
