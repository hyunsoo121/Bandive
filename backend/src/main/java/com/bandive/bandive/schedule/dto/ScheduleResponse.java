package com.bandive.bandive.schedule.dto;

import java.time.Instant;
import java.util.List;

import com.bandive.bandive.schedule.Attendance;
import com.bandive.bandive.schedule.AttendanceStatus;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleType;

public record ScheduleResponse(Long id, Long bandId, ScheduleType type, Instant dateTime, String location,
		Long createdByUserId, String createdByNickname, Counts counts, AttendanceStatus myStatus,
		List<AttendeeResponse> attendees, Instant createdAt) {

	/** 응답한 멤버들의 상태 집계 (미응답자는 어디에도 없음). */
	public record Counts(long attending, long absent, long undecided) {
	}

	public static ScheduleResponse from(Schedule schedule, List<Attendance> attendances, Long currentUserId) {
		long attending = 0;
		long absent = 0;
		long undecided = 0;
		AttendanceStatus myStatus = null;
		List<AttendeeResponse> attendees = attendances.stream().map(AttendeeResponse::from).toList();
		for (Attendance attendance : attendances) {
			switch (attendance.getStatus()) {
				case ATTENDING -> attending++;
				case ABSENT -> absent++;
				case UNDECIDED -> undecided++;
			}
			if (currentUserId != null && attendance.getUser().getId().equals(currentUserId)) {
				myStatus = attendance.getStatus();
			}
		}
		return new ScheduleResponse(schedule.getId(), schedule.getBand().getId(), schedule.getType(),
				schedule.getDateTime(), schedule.getLocation(), schedule.getCreatedBy().getId(),
				schedule.getCreatedBy().getNickname(), new Counts(attending, absent, undecided), myStatus, attendees,
				schedule.getCreatedAt());
	}

}
