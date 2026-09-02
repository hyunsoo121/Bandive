import { api, loginUrl, setAccessToken, tryRefresh } from './client';
import type { MeDto } from './types';

/** 카카오 로그인 시작 — 브라우저를 백엔드 인가 엔드포인트로 보낸다. 성공 시 /oauth/success 로 복귀. */
export function startKakaoLogin(): void {
  window.location.href = loginUrl();
}

/** refresh 쿠키로 access 토큰을 복구. 로그인 상태면 true. */
export function restoreSession(): Promise<boolean> {
  return tryRefresh();
}

export function fetchMe(): Promise<MeDto> {
  return api.get<MeDto>('/api/auth/me');
}

export async function logout(): Promise<void> {
  try {
    await api.post<void>('/api/auth/logout');
  } finally {
    setAccessToken(null);
  }
}
