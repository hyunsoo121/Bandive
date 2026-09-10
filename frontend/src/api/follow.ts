import { api } from './client';
import type { FollowerDto, FollowStatusDto } from './types';

/** 팔로우 요청 (FOLLOWERS 밴드만). */
export const requestFollow = (bandId: string) => api.post<void>(`/api/bands/${bandId}/follow`);

/** 요청 취소 / 언팔로우. */
export const cancelFollow = (bandId: string) => api.del<void>(`/api/bands/${bandId}/follow`);

/** 관리자 — 팔로워/요청 목록. status 생략 시 전체. */
export const listFollowers = (bandId: string, status?: FollowStatusDto) =>
  api.get<FollowerDto[]>(`/api/bands/${bandId}/followers${status ? `?status=${status}` : ''}`);

/** 관리자 — 요청 승인. */
export const approveFollower = (bandId: string, userId: string) =>
  api.put<void>(`/api/bands/${bandId}/followers/${userId}`);

/** 관리자 — 요청 거절 / 팔로워 제거. */
export const removeFollower = (bandId: string, userId: string) =>
  api.del<void>(`/api/bands/${bandId}/followers/${userId}`);
