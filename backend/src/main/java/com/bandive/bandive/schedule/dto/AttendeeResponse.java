package com.bandive.bandive.schedule.dto;

import com.bandive.bandive.schedule.Attendance;
import com.bandive.bandive.schedule.AttendanceStatus;

/**
 * 참석자 한 건. 대상은 실멤버(userId) 또는 게스트(guestId) 중 하나 — 나머지는 null. nickname 은 그 쪽의 표시 이름. 게스트의
 * 세션은 밴드 게스트 목록(GuestResponse)에 있으므로 여기엔 담지 않는다.
 */
public record AttendeeResponse(Long userId, Long guestId, String nickname, AttendanceStatus status) {

	public static AttendeeResponse from(Attendance attendance) {
		if (attendance.getGuest() != null) {
			return new AttendeeResponse(null, attendance.getGuest().getId(), attendance.getGuest().getName(),
					attendance.getStatus());
		}
		return new AttendeeResponse(attendance.getUser().getId(), null, attendance.getUser().getNickname(),
				attendance.getStatus());
	}

}
