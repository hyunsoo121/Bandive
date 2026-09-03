// 일정 도메인 헬퍼 — 백엔드 enum ↔ 화면 표기(한글) 변환, 날짜 파생값.

import type { AttendanceStatusDto } from '../api/types';
import type { AttendanceStatus, ScheduleEvent, ScheduleType } from '../types';

export const KIND_LABEL: Record<ScheduleType, string> = {
  REHEARSAL: '연습',
  PERFORMANCE: '공연',
};

export const ATT_TO_KO: Record<AttendanceStatusDto, AttendanceStatus> = {
  ATTENDING: '참석',
  UNDECIDED: '미정',
  ABSENT: '불참',
};

export const ATT_TO_EN: Record<AttendanceStatus, AttendanceStatusDto> = {
  참석: 'ATTENDING',
  미정: 'UNDECIDED',
  불참: 'ABSENT',
};

const DOW = ['일', '월', '화', '수', '목', '금', '토'];

/** ScheduleEvent 에 캘린더/리스트에서 쓰는 날짜 파생값을 붙인다. */
export interface UiSchedule extends ScheduleEvent {
  at: Date;
  day: number;
  month: number;
  year: number;
  dow: string;
  timeLabel: string;
}

export function toUi(e: ScheduleEvent): UiSchedule {
  const at = new Date(e.dateTime);
  return {
    ...e,
    at,
    day: at.getDate(),
    month: at.getMonth(),
    year: at.getFullYear(),
    dow: DOW[at.getDay()],
    timeLabel: at.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }),
  };
}

export const byDateAsc = (a: ScheduleEvent, b: ScheduleEvent) =>
  a.dateTime.localeCompare(b.dateTime);

/** 지금 이후 가장 가까운 일정. */
export function nextSchedule(list: ScheduleEvent[], now: Date = new Date()): ScheduleEvent | null {
  return [...list].sort(byDateAsc).find((e) => new Date(e.dateTime) >= now) ?? null;
}
