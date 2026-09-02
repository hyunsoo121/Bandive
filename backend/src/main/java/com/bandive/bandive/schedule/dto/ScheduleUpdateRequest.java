package com.bandive.bandive.schedule.dto;

import java.time.Instant;

import jakarta.validation.constraints.Size;

import com.bandive.bandive.schedule.ScheduleType;

/**
 * 부분 수정 — null 인 필드는 변경하지 않는다.
 */
public record ScheduleUpdateRequest(ScheduleType type, Instant dateTime,
		@Size(max = 200, message = "장소는 200자 이내여야 합니다") String location) {
}
