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

/** 일정 삭제 (관리자). attendances cascade. */
export const deleteSchedule = (scheduleId: string) => api.del<void>(`/api/schedules/${scheduleId}`);

/** 내 참석 여부 등록/변경 (밴드 멤버) — upsert. */
export const setAttendance = (scheduleId: string, status: AttendanceStatusDto) =>
  api.post<ScheduleDto>(`/api/schedules/${scheduleId}/attendance`, { status });

/** 관리자가 특정 멤버의 참석 여부를 대신 등록/변경 — upsert. */
export const setMemberAttendance = (
  scheduleId: string,
  userId: string,
  status: AttendanceStatusDto,
) => api.post<ScheduleDto>(`/api/schedules/${scheduleId}/attendance/${userId}`, { status });

/** 관리자가 게스트를 일정에 추가 (이미 있으면 그대로). 게스트는 항상 참석. */
export const setGuestAttendance = (scheduleId: string, guestId: string) =>
  api.post<ScheduleDto>(`/api/schedules/${scheduleId}/attendance/guests/${guestId}`);

/** 관리자가 게스트를 일정에서 제외. */
export const clearGuestAttendance = (scheduleId: string, guestId: string) =>
  api.del<ScheduleDto>(`/api/schedules/${scheduleId}/attendance/guests/${guestId}`);
