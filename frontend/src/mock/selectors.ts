// mock 데이터에서 밴드별로 뽑아 쓰는 헬퍼. 백엔드 연동 시 각 API 호출로 대체.

import { SCHEDULES } from './data';
import type { ScheduleEvent } from '../types';

// 곡·영상·멤버는 AppContext(songs, media, members) 상태에서 관리 → selector 없음 (화면에서 bandId 로 필터).
export const schedulesOfBand = (bandId: string): ScheduleEvent[] =>
  SCHEDULES.filter((e) => e.bandId === bandId).sort((a, b) => a.day - b.day);

/** 오늘(2026-08-28) 기준 다가오는 가장 가까운 일정 */
export function nextSchedule(bandId: string, todayDay = 28): ScheduleEvent | null {
  const upcoming = schedulesOfBand(bandId).filter((e) => e.day >= todayDay);
  return upcoming[0] ?? null;
}

export const KIND_LABEL: Record<ScheduleEvent['type'], string> = {
  REHEARSAL: '연습',
  PERFORMANCE: '공연',
};
