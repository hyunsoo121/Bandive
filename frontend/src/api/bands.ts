import { api } from './client';
import type { BandDto } from './types';

/** 밴드 상세 — 공개(GET). 비회원/비멤버도 열람 가능. */
export const getBand = (bandId: string) => api.get<BandDto>(`/api/bands/${bandId}`);

/** 내가 속한 밴드 목록 — 인증 필요. */
export const getMyBands = () => api.get<BandDto[]>('/api/bands/my');

export const createBand = (name: string, description?: string | null) =>
  api.post<BandDto>('/api/bands', { name, description: description ?? null });

export const updateBand = (bandId: string, name: string, description: string | null) =>
  api.patch<BandDto>(`/api/bands/${bandId}`, { name, description });

function fileForm(file: File): FormData {
  const form = new FormData();
  form.append('file', file);
  return form;
}

export const uploadLogo = (bandId: string, file: File) =>
  api.upload<BandDto>(`/api/bands/${bandId}/logo`, fileForm(file));

export const uploadBanner = (bandId: string, file: File) =>
  api.upload<BandDto>(`/api/bands/${bandId}/banner`, fileForm(file));
