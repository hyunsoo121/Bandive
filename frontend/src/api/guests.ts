import { api } from './client';
import type { GuestDto } from './types';

/** 게스트 목록 — 공개(GET). 이름순. */
export const listGuests = (bandId: string) => api.get<GuestDto[]>(`/api/bands/${bandId}/guests`);

/** 게스트 등록 (관리자). */
export const createGuest = (bandId: string, name: string) =>
  api.post<GuestDto>(`/api/bands/${bandId}/guests`, { name });

/** 게스트 이름 수정 (관리자). */
export const renameGuest = (bandId: string, guestId: string, name: string) =>
  api.patch<GuestDto>(`/api/bands/${bandId}/guests/${guestId}`, { name });

/** 게스트 삭제 (관리자). 세션 배정은 자동 해제, 출결 행은 삭제된다. */
export const deleteGuest = (bandId: string, guestId: string) =>
  api.del<void>(`/api/bands/${bandId}/guests/${guestId}`);
