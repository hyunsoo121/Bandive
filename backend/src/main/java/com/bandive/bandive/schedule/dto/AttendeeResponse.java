package com.bandive.bandive.schedule.dto;

import com.bandive.bandive.schedule.Attendance;
import com.bandive.bandive.schedule.AttendanceStatus;

/**
 * 참석자 한 건. 대상은 실멤버(userId) 또는 게스트(guestId) 중 하나 — 나머지는 null. nickname 은 배정된 쪽의 표시 이름.
 * session 은 게스트가 그 일정에서 맡는 세션(악기 또는 "관객"), 실멤버는 항상 null.
 */
public record AttendeeResponse(Long userId, Long guestId, String nickname, AttendanceStatus status, String session) {

	public static AttendeeResponse from(Attendance attendance) {
		if (attendance.getGuest() != null) {
			return new AttendeeResponse(null, attendance.getGuest().getId(), attendance.getGuest().getName(),
					attendance.getStatus(), attendance.getSession());
		}
		return new AttendeeResponse(attendance.getUser().getId(), null, attendance.getUser().getNickname(),
				attendance.getStatus(), null);
	}

}
