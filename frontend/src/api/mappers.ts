// 백엔드 DTO → 프론트 도메인 타입(../types). 목업 시절 화면이 기대하는 모양을 그대로 유지한다.

import type { Band, Member, User } from '../types';
import type { BandDto, BandRoleDto, MemberDto, MeDto } from './types';

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
