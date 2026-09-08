import { api, loginUrl, setAccessToken, tryRefresh } from './client';
import type { AccessTokenDto, MeDto } from './types';

/** 카카오 로그인 시작 — 브라우저를 백엔드 인가 엔드포인트로 보낸다. 성공 시 /oauth/success 로 복귀. */
export function startKakaoLogin(): void {
  window.location.href = loginUrl();
}

/** 이메일 회원가입 → 바로 로그인 상태 (access 토큰을 메모리에 저장). */
export async function signupWithEmail(
  email: string,
  password: string,
  nickname: string,
): Promise<void> {
  const r = await api.post<AccessTokenDto>('/api/auth/signup', { email, password, nickname });
  setAccessToken(r.accessToken);
}

/** 이메일 로그인. */
export async function loginWithEmail(email: string, password: string): Promise<void> {
  const r = await api.post<AccessTokenDto>('/api/auth/login', { email, password });
  setAccessToken(r.accessToken);
}

/** refresh 쿠키로 access 토큰을 복구. 로그인 상태면 true. */
export function restoreSession(): Promise<boolean> {
  return tryRefresh();
}

export function fetchMe(): Promise<MeDto> {
  return api.get<MeDto>('/api/auth/me');
}

/** 내 정보 수정 (현재는 닉네임만). */
export function updateMe(nickname: string): Promise<MeDto> {
  return api.patch<MeDto>('/api/auth/me', { nickname });
}

/** 비밀번호 변경 (이메일 로그인 계정만). 현재 비밀번호가 맞아야 함. */
export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return api.patch<void>('/api/auth/me/password', { currentPassword, newPassword });
}

export async function logout(): Promise<void> {
  try {
    await api.post<void>('/api/auth/logout');
  } finally {
    setAccessToken(null);
  }
}
