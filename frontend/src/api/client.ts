// fetch 래퍼. access 토큰은 메모리에만 두고, 401 이면 refresh 쿠키로 한 번 재발급 후 재시도한다.
// refresh 토큰은 httpOnly 쿠키(SameSite=Strict, path /api/auth)라 JS 에서 못 만지고 credentials:'include' 로만 실린다.

import { ApiError, type AccessTokenDto, type ErrorBody } from './types';

/** 백엔드 오리진. 개발 기본값은 docker-compose 앱 포트(8081). 배포 시 VITE_API_ORIGIN 으로 덮어쓴다. */
const API_ORIGIN = import.meta.env.VITE_API_ORIGIN ?? 'http://localhost:8081';

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/** 카카오 로그인 시작 URL (브라우저를 통째로 여기로 보낸다). */
export function loginUrl(): string {
  return `${API_ORIGIN}/oauth2/authorization/kakao`;
}

/** 백엔드가 내려준 상대 파일 경로(/files/...)를 절대 URL 로. */
export function fileUrl(path: string | null | undefined): string {
  if (!path) return '';
  return path.startsWith('http') ? path : `${API_ORIGIN}${path}`;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** multipart 업로드 */
  form?: FormData;
  /** 401 자동 재시도를 끈다 (refresh 호출 자신). */
  noRetry?: boolean;
}

async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let body: BodyInit | undefined;
  if (opts.form) {
    body = opts.form;
  } else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(opts.body);
  }

  const res = await fetch(`${API_ORIGIN}${path}`, {
    method: opts.method ?? 'GET',
    headers,
    body,
    credentials: 'include',
  });

  if (res.status === 401 && !opts.noRetry && path !== '/api/auth/refresh') {
    const renewed = await tryRefresh();
    if (renewed) return request<T>(path, { ...opts, noRetry: true });
  }

  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as ErrorBody;
    if (body && typeof body.code === 'string') return new ApiError(body);
  } catch {
    // 본문이 JSON 이 아니면 아래 기본 메시지로
  }
  return new ApiError({
    status: res.status,
    code: 'UNKNOWN',
    message: `요청에 실패했습니다 (${res.status})`,
    path: '',
    timestamp: new Date().toISOString(),
  });
}

let refreshing: Promise<boolean> | null = null;

/** refresh 쿠키로 새 access 를 받아 메모리에 저장. 동시에 여러 번 불려도 한 번만 요청한다. */
export function tryRefresh(): Promise<boolean> {
  if (!refreshing) {
    refreshing = request<AccessTokenDto>('/api/auth/refresh', { method: 'POST', noRetry: true })
      .then((r) => {
        accessToken = r.accessToken;
        return true;
      })
      .catch(() => {
        accessToken = null;
        return false;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  upload: <T>(path: string, form: FormData) => request<T>(path, { method: 'POST', form }),
};
