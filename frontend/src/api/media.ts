import { api } from './client';
import type { MediaCreateBody, MediaDto, MediaVisibilityDto } from './types';

/** 영상 목록 — 공개(GET). 공개범위 필터 적용(비회원·비멤버는 LINK_PUBLIC 만). scheduleId 로 일정 필터. */
export const listMedia = (bandId: string, scheduleId?: string) =>
  api.get<MediaDto[]>(`/api/bands/${bandId}/media${scheduleId ? `?scheduleId=${scheduleId}` : ''}`);

/** 영상 URL 등록 (밴드 멤버). platform 은 URL 로 자동 판별, 기본 visibility MEMBERS_ONLY. */
export const addMedia = (bandId: string, body: MediaCreateBody) =>
  api.post<MediaDto>(`/api/bands/${bandId}/media`, body);

/** 공개 범위 변경 (밴드장). */
export const changeVisibility = (mediaId: string, visibility: MediaVisibilityDto) =>
  api.patch<MediaDto>(`/api/media/${mediaId}/visibility`, { visibility });

/** 영상 삭제 (등록자 본인 또는 밴드장). */
export const deleteMedia = (mediaId: string) => api.del<void>(`/api/media/${mediaId}`);
