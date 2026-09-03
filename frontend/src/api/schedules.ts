import { api } from './client';
import type { AttendanceStatusDto, ScheduleDto, ScheduleTypeDto } from './types';

/** 일정 목록 — 공개(GET). dateTime 오름차순, 연결 영상·출결 집계 포함. */
export const listSchedules = (bandId: string) =>
  api.get<ScheduleDto[]>(`/api/bands/${bandId}/schedules`);

export interface ScheduleCreateBody {
  type: ScheduleTypeDto;
  /** ISO-8601 (Instant) */
  dateTime: string;
  location?: string;
}

/** 일정 등록 (밴드 멤버). */
export const createSchedule = (bandId: string, body: ScheduleCreateBody) =>
  api.post<ScheduleDto>(`/api/bands/${bandId}/schedules`, body);

/** 일정 부분 수정 (밴드 멤버 누구나). null 필드는 무시. */
export const updateSchedule = (scheduleId: string, body: Partial<ScheduleCreateBody>) =>
  api.patch<ScheduleDto>(`/api/schedules/${scheduleId}`, body);

/** 일정 삭제 (밴드장). attendances cascade. */
export const deleteSchedule = (scheduleId: string) => api.del<void>(`/api/schedules/${scheduleId}`);

/** 내 참석 여부 등록/변경 (밴드 멤버) — upsert. */
export const setAttendance = (scheduleId: string, status: AttendanceStatusDto) =>
  api.post<ScheduleDto>(`/api/schedules/${scheduleId}/attendance`, { status });
