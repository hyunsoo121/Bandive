// 백엔드(Spring) 응답 DTO 형태. 프론트 도메인 타입(../types)과는 ./mappers 로 이어붙인다.

export interface ErrorBody {
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp: string;
}

/** 백엔드 공통 에러 스키마(ErrorResponse)를 감싼 예외. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(body: ErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = body.status;
    this.code = body.code;
  }
}

export type BandRoleDto = 'OWNER' | 'MEMBER';

export interface BandDto {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  memberCount: number;
  /** GET /api/bands/my 에서만 채워질 예정. 없으면 매퍼가 'member' 로 fallback. */
  role?: BandRoleDto;
  createdAt: string;
}

export interface MemberDto {
  userId: number;
  nickname: string;
  role: BandRoleDto;
  joinedAt: string;
}

export interface InviteCodeDto {
  code: string;
  inviteUrl: string;
  expiresAt: string | null;
  maxUses: number | null;
  usedCount: number;
}

export interface MeDto {
  id: number;
  nickname: string;
}

export interface AccessTokenDto {
  accessToken: string;
  expiresIn: number;
}
