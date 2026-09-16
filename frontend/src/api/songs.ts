import { api } from './client';
import type {
  SongCreateBody,
  SongDto,
  SongStatusDto,
  SongUpdateBody,
  TrackSearchResultDto,
  VoteResultDto,
} from './types';

/** 한 그룹(status × 폴더/미분류) 안 곡 순서 재지정 (밴드 멤버 누구나). songIds 는 그 그룹의 전체 곡. */
export const reorderSongs = (
  bandId: string,
  status: SongStatusDto,
  folderId: string | null,
  songIds: string[],
) =>
  api.put<void>(`/api/bands/${bandId}/songs/order`, {
    status,
    folderId: folderId ? Number(folderId) : null,
    songIds: songIds.map(Number),
  });

/** 외부 음원 검색 (공개). 결과의 externalTrackId 를 곡 추가 시 그대로 넘긴다. */
export const searchTracks = (q: string) =>
  api.get<TrackSearchResultDto[]>(`/api/songs/search?q=${encodeURIComponent(q)}`);

/** 곡 목록 — 공개(GET). status 로 위시/확정 필터. */
export const listSongs = (bandId: string, status?: SongStatusDto) =>
  api.get<SongDto[]>(`/api/bands/${bandId}/songs${status ? `?status=${status}` : ''}`);

/** 곡 추가 (밴드 멤버). 항상 WISHLIST 로 생성된다. */
export const addSong = (bandId: string, body: SongCreateBody) =>
  api.post<SongDto>(`/api/bands/${bandId}/songs`, body);

/** 투표 (멱등) — 1인 1표. */
export const voteSong = (songId: string) => api.post<VoteResultDto>(`/api/songs/${songId}/vote`);

/** 투표 취소 (멱등). */
export const unvoteSong = (songId: string) => api.del<VoteResultDto>(`/api/songs/${songId}/vote`);

/**
 * 부분 수정 (제목/아티스트/메모/참고영상) — 등록자 본인 또는 관리자. 위시리스트·합주곡 모두 가능. 보낸 필드만 반영.
 */
export const updateSong = (songId: string, body: SongUpdateBody) =>
  api.patch<SongDto>(`/api/songs/${songId}`, body);

/** WISHLIST → CONFIRMED 승격 (관리자). */
export const confirmSong = (songId: string) => api.patch<SongDto>(`/api/songs/${songId}/confirm`);

/**
 * 파트 배정/해제 (밴드 멤버 누구나). 곡이 CONFIRMED 여야 한다.
 * target 이 null 이면 해제. 실멤버는 `{ userId }`, 게스트는 `{ guestId }` — 최대 하나.
 */
export const assignPart = (
  songId: string,
  partId: string,
  target: { userId?: string | null; guestId?: string | null } | null,
) =>
  api.put<SongDto>(`/api/songs/${songId}/parts/${partId}/assign`, {
    userId: target?.userId ? Number(target.userId) : null,
    guestId: target?.guestId ? Number(target.guestId) : null,
  });

/** 곡 삭제 (관리자). parts·votes cascade. */
export const deleteSong = (songId: string) => api.del<void>(`/api/songs/${songId}`);

/** 곡을 폴더로 이동 (밴드 멤버 누구나). folderId null 이면 미분류. 대상 그룹 맨 끝에 놓인다. */
export const moveSongToFolder = (songId: string, folderId: string | null) =>
  api.put<SongDto>(`/api/songs/${songId}/folder`, { folderId: folderId ? Number(folderId) : null });
