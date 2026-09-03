import { api } from './client';
import type {
  SongCreateBody,
  SongDto,
  SongStatusDto,
  TrackSearchResultDto,
  VoteResultDto,
} from './types';

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

/** WISHLIST → CONFIRMED 승격 (밴드장). */
export const confirmSong = (songId: string) => api.patch<SongDto>(`/api/songs/${songId}/confirm`);

/** 파트 배정/해제 (밴드 멤버 누구나). userId 가 null 이면 해제. 곡이 CONFIRMED 여야 한다. */
export const assignPart = (songId: string, partId: string, userId: string | null) =>
  api.put<SongDto>(`/api/songs/${songId}/parts/${partId}/assign`, {
    userId: userId ? Number(userId) : null,
  });

/** 곡 삭제 (밴드장). parts·votes cascade. */
export const deleteSong = (songId: string) => api.del<void>(`/api/songs/${songId}`);
