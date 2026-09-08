import { api } from './client';
import type { MemberDto } from './types';

/** 멤버 목록 — 공개(GET). */
export const listMembers = (bandId: string) => api.get<MemberDto[]>(`/api/bands/${bandId}/members`);

/** 멤버 추방 (밴드장). */
export const kickMember = (bandId: string, userId: string) =>
  api.del<void>(`/api/bands/${bandId}/members/${userId}`);

/** 밴드 탈퇴 (본인). */
export const leaveBand = (bandId: string) => api.del<void>(`/api/bands/${bandId}/members/me`);

/** 밴드장 위임 (현재 밴드장). 넘겨받는 사람은 그 밴드 멤버여야 함. */
export const transferOwnership = (bandId: string, userId: string) =>
  api.put<void>(`/api/bands/${bandId}/owner`, { userId: Number(userId) });

/** 밴드 삭제 (밴드장). 곡·일정·영상·멤버 전부 함께 삭제됨. */
export const deleteBand = (bandId: string) => api.del<void>(`/api/bands/${bandId}`);
