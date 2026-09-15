import { api } from './client';
import type { FollowerDto, FollowingBandDto, FollowStatusDto, PublicFollowerDto } from './types';

/** 팔로우 요청 (PRIVATE 밴드만 불가). FOLLOWERS 는 승인 대기, PUBLIC 은 즉시 승인. */
export const requestFollow = (bandId: string) => api.post<void>(`/api/bands/${bandId}/follow`);

/** 요청 취소 / 언팔로우. */
export const cancelFollow = (bandId: string) => api.del<void>(`/api/bands/${bandId}/follow`);

/** 내가 팔로우한 밴드 목록 (요청 대기 + 승인). 로그인 필요. */
export const myFollowing = () => api.get<FollowingBandDto[]>('/api/me/following');

/** 관리자 — 팔로워/요청 목록. status 생략 시 전체. */
export const listFollowers = (bandId: string, status?: FollowStatusDto) =>
  api.get<FollowerDto[]>(`/api/bands/${bandId}/followers${status ? `?status=${status}` : ''}`);

/** 공개 팔로워 목록 (승인된 팔로워만) — 이 밴드 콘텐츠를 볼 수 있는 사람이면 누구나(비로그인 포함). */
export const listPublicFollowers = (bandId: string) =>
  api.get<PublicFollowerDto[]>(`/api/bands/${bandId}/followers/public`);

/** 관리자 — 요청 승인. */
export const approveFollower = (bandId: string, userId: string) =>
  api.put<void>(`/api/bands/${bandId}/followers/${userId}`);

/** 관리자 — 요청 거절 / 팔로워 제거. */
export const removeFollower = (bandId: string, userId: string) =>
  api.del<void>(`/api/bands/${bandId}/followers/${userId}`);
