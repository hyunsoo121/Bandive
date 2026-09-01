// 하단 탭바 / 사이드바 공통 내비게이션 정의.
// path 는 밴드 스코프 하위 상대 경로. (예: /bands/b1/songs)

export interface NavItem {
  key: string;
  label: string;
  /** 밴드 루트 기준 상대 경로. 홈은 '' */
  to: string;
  /** 아이콘 SVG path (2개) */
  d1: string;
  d2: string;
}

export const NAV_ITEMS: NavItem[] = [
  { key: 'home', label: '홈', to: '', d1: 'M3 10.5 12 3l9 7.5', d2: 'M5 9.5V21h14V9.5' },
  {
    key: 'songs',
    label: '곡',
    to: 'songs',
    d1: 'M10 18V5l11-2v12',
    d2: 'M7 15a3 3 0 1 0 0 6 3 3 0 1 0 0-6M18 12a3 3 0 1 0 0 6 3 3 0 1 0 0-6',
  },
  {
    key: 'schedule',
    label: '일정',
    to: 'schedule',
    d1: 'M3 5h18v16H3z',
    d2: 'M3 10h18M8 3v4M16 3v4',
  },
  { key: 'media', label: '영상', to: 'media', d1: 'M2 6h14v12H2z', d2: 'm16 11 6-4v10z' },
  {
    key: 'members',
    label: '멤버',
    to: 'members',
    d1: 'M12 4a4 4 0 1 0 0 8 4 4 0 1 0 0-8',
    d2: 'M4 21c0-4 3.6-6 8-6s8 2 8 6',
  },
];
