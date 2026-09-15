// 백엔드 DTO → 프론트 도메인 타입(../types). 목업 시절 화면이 기대하는 모양을 그대로 유지한다.

import { fileUrl } from './client';
import { ATT_TO_KO } from '../lib/schedule';
import type {
  Band,
  Follower,
  FollowingBand,
  Guest,
  MediaItem,
  Member,
  NotificationItem,
  PublicFollower,
  ScheduleEvent,
  SessionShape,
  Song,
  User,
  UserProfile,
} from '../types';
import type {
  BandDto,
  BandRoleDto,
  FollowerDto,
  FollowingBandDto,
  GuestDto,
  MediaDto,
  MediaPlatformDto,
  MemberDto,
  MeDto,
  NotificationDto,
  PublicFollowerDto,
  ScheduleDto,
  SongDto,
  UserProfileDto,
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
  return {
    id: String(dto.id),
    name: dto.nickname,
    initial: initialOf(dto.nickname),
    email: dto.email,
    avatarUrl: dto.avatarUrl ? fileUrl(dto.avatarUrl) : null,
    bio: dto.bio,
    loginProvider: dto.provider === 'LOCAL' ? 'local' : 'kakao',
  };
}

export function toUserProfile(dto: UserProfileDto): UserProfile {
  return {
    id: String(dto.id),
    name: dto.nickname,
    initial: initialOf(dto.nickname),
    avatarUrl: dto.avatarUrl ? fileUrl(dto.avatarUrl) : null,
    bio: dto.bio,
    bands: dto.bands.map((b) => ({
      id: String(b.id),
      name: b.name,
      initial: initialOf(b.name),
      logoUrl: b.logoUrl ? fileUrl(b.logoUrl) : null,
      memberCount: b.memberCount,
      myRole: b.role === 'OWNER' ? 'owner' : 'member',
    })),
  };
}

export function toBand(dto: BandDto): Band {
  return {
    id: String(dto.id),
    name: dto.name,
    initial: initialOf(dto.name),
    memberCount: dto.memberCount,
    followerCount: dto.followerCount ?? 0,
    myRole: toRole(dto.role),
    note: dto.description ?? '',
    logoUrl: dto.logoUrl ? fileUrl(dto.logoUrl) : null,
    bannerUrl: dto.bannerUrl ? fileUrl(dto.bannerUrl) : null,
    visibility: dto.visibility ?? 'PUBLIC',
    myRelation: dto.myRelation ?? 'NONE',
  };
}

export function toFollower(dto: FollowerDto): Follower {
  return {
    userId: String(dto.userId),
    nickname: dto.nickname,
    initial: initialOf(dto.nickname),
    status: dto.status,
    requestedAt: dto.requestedAt,
    decidedAt: dto.decidedAt,
  };
}

export function toPublicFollower(dto: PublicFollowerDto): PublicFollower {
  return {
    userId: String(dto.userId),
    nickname: dto.nickname,
    initial: initialOf(dto.nickname),
    avatarUrl: dto.avatarUrl ? fileUrl(dto.avatarUrl) : null,
  };
}

export function toNotificationItem(dto: NotificationDto): NotificationItem {
  return {
    id: String(dto.id),
    type: dto.type,
    bandId: String(dto.bandId),
    bandName: dto.bandName,
    actorId: dto.actorId != null ? String(dto.actorId) : null,
    actorNickname: dto.actorNickname,
    readAt: dto.readAt,
    createdAt: dto.createdAt,
  };
}

export function toFollowingBand(dto: FollowingBandDto): FollowingBand {
  return {
    bandId: String(dto.bandId),
    name: dto.name,
    initial: initialOf(dto.name),
    logoUrl: dto.logoUrl ? fileUrl(dto.logoUrl) : null,
    visibility: dto.visibility,
    memberCount: dto.memberCount,
    status: dto.status,
    requestedAt: dto.requestedAt,
  };
}

export function toGuest(dto: GuestDto): Guest {
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    name: dto.name,
    session: dto.session ?? null,
  };
}

export function toMember(dto: MemberDto, bandId: string): Member {
  return {
    id: String(dto.userId),
    bandId,
    name: dto.nickname,
    initial: initialOf(dto.nickname),
    avatarUrl: dto.avatarUrl ? fileUrl(dto.avatarUrl) : null,
    parts: dto.parts ?? [],
    leader: dto.leader ?? false,
    role: toRole(dto.role),
    avatarColor: avatarColor(dto.userId),
  };
}

export function toSong(dto: SongDto): Song {
  const sessions: SessionShape = {};
  const assignments: Record<string, string> = {};
  for (const p of dto.parts) {
    const inst = p.instrument;
    sessions[inst] = (sessions[inst] ?? 0) + 1;
    if (p.assignedName) assignments[`${p.instrument}#${p.partIndex}`] = p.assignedName;
  }
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    title: dto.title,
    artist: dto.artist ?? '',
    status: dto.status,
    sourceType: dto.sourceType,
    externalTrackId: dto.externalTrackId ?? null,
    proposer: dto.addedByNickname,
    addedByUserId: String(dto.addedByUserId),
    memo: dto.memo ?? '',
    referenceVideoUrl: dto.referenceVideoUrl ?? '',
    artworkUrl: dto.artworkUrl,
    sessions,
    votes: dto.voteCount,
    votedByMe: dto.votedByMe,
    folderId: dto.folderId != null ? String(dto.folderId) : null,
    position: dto.position ?? 0,
    addedOrder: Date.parse(dto.createdAt) || dto.id,
    assignments,
    parts: dto.parts.map((p) => ({
      id: String(p.id),
      instrument: p.instrument,
      partIndex: p.partIndex,
      assigneeId: p.assignedUserId != null ? String(p.assignedUserId) : null,
      assigneeGuestId: p.assignedGuestId != null ? String(p.assignedGuestId) : null,
      assigneeName: p.assignedName,
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
      userId: a.userId != null ? String(a.userId) : null,
      guestId: a.guestId != null ? String(a.guestId) : null,
      nickname: a.nickname,
      status: ATT_TO_KO[a.status],
    })),
    mediaIds: dto.media.map((m) => String(m.id)),
  };
}

export function toSongFolder(dto: {
  id: number;
  name: string;
  status: 'WISHLIST' | 'CONFIRMED';
  position: number;
}): import('../types').SongFolder {
  return { id: String(dto.id), name: dto.name, status: dto.status, position: dto.position };
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

const PLATFORM_KEY: Record<MediaPlatformDto, MediaItem['platform']> = {
  YOUTUBE: 'youtube',
  GOOGLE_DRIVE: 'drive',
  OTHER: 'other',
};

export function toMedia(dto: MediaDto): MediaItem {
  return {
    id: String(dto.id),
    bandId: String(dto.bandId),
    url: dto.externalUrl,
    title: dto.title?.trim() || prettyUrl(dto.externalUrl),
    rawTitle: dto.title?.trim() || null,
    thumbnailUrl: dto.thumbnailUrl,
    source: PLATFORM_LABEL[dto.platform],
    platform: PLATFORM_KEY[dto.platform],
    date: shortDate(dto.createdAt),
    createdAtMs: Date.parse(dto.createdAt) || 0,
    kind: dto.type === 'PERFORMANCE' ? '공연' : '합주',
    visibility: dto.visibility === 'LINK_PUBLIC' ? '전체공개' : '멤버만',
    uploadedByUserId: String(dto.uploadedByUserId),
    scheduleId: dto.scheduleId != null ? String(dto.scheduleId) : null,
    songId: dto.songId != null ? String(dto.songId) : null,
    songTitle: dto.songTitle,
    likeCount: dto.likeCount,
    likedByMe: dto.likedByMe,
  };
}
