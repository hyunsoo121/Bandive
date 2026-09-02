import { api } from './client';
import type { MemberDto } from './types';

/** 멤버 목록 — 공개(GET). */
export const listMembers = (bandId: string) => api.get<MemberDto[]>(`/api/bands/${bandId}/members`);

/** 멤버 추방 (밴드장). */
export const kickMember = (bandId: string, userId: string) =>
  api.del<void>(`/api/bands/${bandId}/members/${userId}`);

/** 밴드 탈퇴 (본인). */
export const leaveBand = (bandId: string) => api.del<void>(`/api/bands/${bandId}/members/me`);
