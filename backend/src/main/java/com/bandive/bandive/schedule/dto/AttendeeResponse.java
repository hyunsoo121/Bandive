package com.bandive.bandive.schedule.dto;

import com.bandive.bandive.schedule.Attendance;
import com.bandive.bandive.schedule.AttendanceStatus;

public record AttendeeResponse(Long userId, String nickname, AttendanceStatus status) {

	public static AttendeeResponse from(Attendance attendance) {
		return new AttendeeResponse(attendance.getUser().getId(), attendance.getUser().getNickname(),
				attendance.getStatus());
	}

}
