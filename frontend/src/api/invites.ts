import { api } from './client';
import type { BandDto, InviteCodeDto, InvitePreviewDto } from './types';

/** 초대 코드 발급/재발급 (관리자). 밴드당 1개라 다시 부르면 이전 코드는 폐기된다. */
export const issueInviteCode = (bandId: string) =>
  api.post<InviteCodeDto>(`/api/bands/${bandId}/invite-codes`);

/** 초대 코드 미리보기 (공개, 가입 전 확인 화면용). 비로그인도 조회 가능. */
export const previewInvite = (code: string) =>
  api.get<InvitePreviewDto>(`/api/invite-codes/${encodeURIComponent(code)}`);

/** 초대 코드로 가입 (로그인). 가입한 밴드를 돌려준다. */
export const joinByCode = (code: string) =>
  api.post<BandDto>(`/api/invite-codes/${encodeURIComponent(code)}/join`);
