import { api } from './client';
import type { MemberDto } from './types';

/** 멤버 목록 — 공개(GET). */
export const listMembers = (bandId: string) => api.get<MemberDto[]>(`/api/bands/${bandId}/members`);

/** 내 세션(파트) 전체 교체. 빈 배열이면 전부 해제. */
export const updateMyParts = (bandId: string, parts: string[]) =>
  api.patch<MemberDto>(`/api/bands/${bandId}/members/me`, { parts });

/** 관리자가 특정 멤버의 세션 전체 교체. */
export const updateMemberParts = (bandId: string, userId: string, parts: string[]) =>
  api.patch<MemberDto>(`/api/bands/${bandId}/members/${userId}`, { parts });

/** 관리자가 밴드 리더 지정/해제 (userId null = 리더 없음). 갱신된 멤버 목록을 돌려준다. */
export const setLeader = (bandId: string, userId: string | null) =>
  api.put<MemberDto[]>(`/api/bands/${bandId}/members/leader`, {
    userId: userId ? Number(userId) : null,
  });

/** 멤버 추방 (관리자). */
export const kickMember = (bandId: string, userId: string) =>
  api.del<void>(`/api/bands/${bandId}/members/${userId}`);

/** 밴드 탈퇴 (본인). */
export const leaveBand = (bandId: string) => api.del<void>(`/api/bands/${bandId}/members/me`);

/** 관리자 위임 (현재 관리자). 넘겨받는 사람은 그 밴드 멤버여야 함. */
export const transferOwnership = (bandId: string, userId: string) =>
  api.put<void>(`/api/bands/${bandId}/owner`, { userId: Number(userId) });

/** 밴드 삭제 (관리자). 곡·일정·영상·멤버 전부 함께 삭제됨. */
export const deleteBand = (bandId: string) => api.del<void>(`/api/bands/${bandId}`);
