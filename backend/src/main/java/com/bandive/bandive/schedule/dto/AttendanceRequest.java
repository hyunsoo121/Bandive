package com.bandive.bandive.schedule.dto;

import jakarta.validation.constraints.NotNull;

import com.bandive.bandive.schedule.AttendanceStatus;

public record AttendanceRequest(
		@NotNull(message = "참석 여부(ATTENDING/UNDECIDED/ABSENT)는 필수입니다") AttendanceStatus status) {
}
