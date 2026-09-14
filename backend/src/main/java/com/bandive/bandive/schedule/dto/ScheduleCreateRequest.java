package com.bandive.bandive.schedule.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.schedule.ScheduleType;

public record ScheduleCreateRequest(@NotNull(message = "일정 종류(REHEARSAL/PERFORMANCE)는 필수입니다") ScheduleType type,
		@NotNull(message = "일시는 필수입니다") Instant dateTime,
		@Size(max = 200, message = "장소는 200자 이내여야 합니다") String location) {
}
