import { api } from './client';
import type { SongStatusDto } from './types';

export interface SongFolderDto {
  id: number;
  name: string;
  status: SongStatusDto;
  position: number;
}

/** 폴더 목록 — 공개(GET). 위시/합주 폴더 모두, status·position 순. */
export const listFolders = (bandId: string) =>
  api.get<SongFolderDto[]>(`/api/bands/${bandId}/song-folders`);

/** 폴더 생성 (관리자) — 목록 끝에 추가. */
export const createFolder = (bandId: string, name: string, status: SongStatusDto) =>
  api.post<SongFolderDto>(`/api/bands/${bandId}/song-folders`, { name, status });

/** 폴더 이름 변경 (관리자). */
export const renameFolder = (folderId: string, name: string) =>
  api.patch<SongFolderDto>(`/api/song-folders/${folderId}`, { name });

/** 폴더 삭제 (관리자) — 소속 곡은 미분류로. */
export const deleteFolder = (folderId: string) => api.del<void>(`/api/song-folders/${folderId}`);

/** 한 status 안에서 폴더 순서 재지정 (관리자). folderIds 는 그 status 의 전체 폴더. */
export const reorderFolders = (bandId: string, status: SongStatusDto, folderIds: string[]) =>
  api.put<SongFolderDto[]>(`/api/bands/${bandId}/song-folders/order`, {
    status,
    folderIds: folderIds.map(Number),
  });
