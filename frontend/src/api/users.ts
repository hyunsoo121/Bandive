import { api } from './client';
import type { UserProfileDto } from './types';

/** 다른 사람 공개 프로필 — 닉네임·사진·한줄소개 + 소속 밴드. 공개 GET. */
export const getUserProfile = (userId: string) => api.get<UserProfileDto>(`/api/users/${userId}`);
