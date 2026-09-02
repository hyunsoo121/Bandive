import { api } from './client';
import type { BandDto, InviteCodeDto } from './types';

/** 초대 코드 발급/재발급 (밴드장). 밴드당 1개라 다시 부르면 이전 코드는 폐기된다. */
export const issueInviteCode = (bandId: string) =>
  api.post<InviteCodeDto>(`/api/bands/${bandId}/invite-codes`);

/** 초대 코드로 가입 (로그인). 가입한 밴드를 돌려준다. */
export const joinByCode = (code: string) =>
  api.post<BandDto>(`/api/invite-codes/${encodeURIComponent(code)}/join`);
