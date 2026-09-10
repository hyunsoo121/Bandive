import { api } from './client';
import type {
  ExploreBandDetailDto,
  ExploreBandDto,
  ExploreTrackDto,
  ExploreVideoDto,
} from './types';

/** 탐색 — 전부 공개 GET (비로그인도 가능). 좋아요만 로그인 필요. */

export const exploreBands = () => api.get<ExploreBandDto[]>('/api/explore/bands');

/** 탐색 안에서 밴드 한 곳 구경. */
export const exploreBand = (bandId: string) =>
  api.get<ExploreBandDetailDto>(`/api/explore/bands/${bandId}`);

export const exploreSongs = () => api.get<ExploreTrackDto[]>('/api/explore/songs');

export const exploreSongVideos = (externalTrackId: string) =>
  api.get<ExploreVideoDto[]>(`/api/explore/songs/${encodeURIComponent(externalTrackId)}/media`);
