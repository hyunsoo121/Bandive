// 백엔드 DTO → 프론트 도메인 타입(../types). 목업 시절 화면이 기대하는 모양을 그대로 유지한다.

import { ATT_TO_KO } from '../lib/schedule';
import type {
  Band,
  Instrument,
  MediaItem,
  Member,
  ScheduleEvent,
  SessionShape,
  Song,
  User,
} from '../types';
import type {
  BandDto,
  BandRoleDto,
  MediaDto,
  MediaPlatformDto,
  MemberDto,
  MeDto,
  ScheduleDto,
  SongDto,
} from './types';

/** 이름의 첫 글자(그래프임 단위) — 이니셜 아바타용. */
export function initialOf(name: string): string {
  return [...name.trim()][0] ?? '밴';
}

const AVATAR_PALETTE = ['#201e1d', '#444141', '#605d5d', '#7d7979', '#9b9797'];

function avatarColor(seed: number): string {
  return AVATAR_PALETTE[Math.abs(seed) % AVATAR_PALETTE.length];
}

const toRole = (role: BandRoleDto | undefined): 'owner' | 'member' =>
  role === 'OWNER' ? 'owner' : 'member';

export function toUser(dto: MeDto): User {
  return { id: String(dto.id), name: dto.nickname, initial: initialOf(dto.nickname) };
}

export function toBand(dto: BandDto): Band {
  return {
    id: String(dto.id),
    name: dto.name,
    initial: initialOf(dto.name),
    memberCount: dto.memberCount,
    myRole: toRole(dto.role),
    note: dto.description ?? '',
  };
}

export function toMember(dto: MemberDto, bandId: string): Member {
  return {
    id: String(dto.userId),
    bandId,
    name: dto.nickname,
    initial: initialOf(dto.nickname),
    part: '',
    role: toRole(dto.role),
    avatarColor: avatarColor(dto.userId),
  };
}

export function toSong(dto: SongDto): Song {
  const sessions: SessionShape = {};
  const assignments: Record<string, string> = {};
  for (const p of dto.parts) {
    const inst = p.instrument as Instrument;
    sessions[inst] = (sessions[inst] ?? 0) + 1;
    if (p.assignedNickname) assignments[`${p.instrument}#${p.partIndex}`] = p.assignedNickname;
  }
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    title: dto.title,
    artist: dto.artist ?? '',
    status: dto.status,
    sourceType: dto.sourceType,
    proposer: dto.addedByNickname,
    memo: dto.memo ?? '',
    referenceVideoUrl: dto.referenceVideoUrl ?? '',
    sessions,
    votes: dto.voteCount,
    votedByMe: dto.votedByMe,
    addedOrder: Date.parse(dto.createdAt) || dto.id,
    assignments,
    parts: dto.parts.map((p) => ({
      id: String(p.id),
      instrument: p.instrument,
      partIndex: p.partIndex,
      assigneeId: p.assignedUserId != null ? String(p.assignedUserId) : null,
      assigneeName: p.assignedNickname,
    })),
  };
}

export function toSchedule(dto: ScheduleDto): ScheduleEvent {
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    type: dto.type,
    dateTime: dto.dateTime,
    location: dto.location ?? '',
    counts: dto.counts,
    myStatus: dto.myStatus ? ATT_TO_KO[dto.myStatus] : null,
    attendees: dto.attendees.map((a) => ({
      userId: String(a.userId),
      nickname: a.nickname,
      status: ATT_TO_KO[a.status],
    })),
    mediaIds: dto.media.map((m) => String(m.id)),
  };
}

const PLATFORM_LABEL: Record<MediaPlatformDto, string> = {
  YOUTUBE: 'YouTube',
  GOOGLE_DRIVE: 'Google Drive',
  OTHER: '링크',
};

/** URL 을 짧은 표시용 라벨로 (프로토콜 제거 + 말줄임). */
function prettyUrl(url: string): string {
  const bare = url.replace(/^https?:\/\//, '').replace(/\/$/, '');
  return bare.length > 44 ? `${bare.slice(0, 44)}…` : bare;
}

function shortDate(iso: string): string {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

export function toMedia(dto: MediaDto): MediaItem {
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    url: dto.externalUrl,
    title: prettyUrl(dto.externalUrl),
    source: PLATFORM_LABEL[dto.platform],
    date: shortDate(dto.createdAt),
    kind: dto.type === 'PERFORMANCE' ? '공연' : '합주',
    visibility: dto.visibility === 'LINK_PUBLIC' ? '링크 공개' : '멤버만',
    scheduleId: dto.scheduleId != null ? String(dto.scheduleId) : null,
  };
}
