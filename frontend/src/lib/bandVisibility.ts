import type { BandVisibility } from '../types';

export const VISIBILITY_ORDER: BandVisibility[] = ['PRIVATE', 'FOLLOWERS', 'PUBLIC'];

export const VISIBILITY_LABEL: Record<BandVisibility, string> = {
  PRIVATE: '비공개',
  FOLLOWERS: '팔로워 공개',
  PUBLIC: '전체공개',
};

export const VISIBILITY_HINT: Record<BandVisibility, string> = {
  PRIVATE: '밴드원만 볼 수 있어요. 탐색에도 안 뜹니다.',
  FOLLOWERS: '밴드원과, 관리자가 승인한 팔로워만 콘텐츠를 볼 수 있어요.',
  PUBLIC: '누구나 볼 수 있고 탐색에 노출됩니다.',
};
