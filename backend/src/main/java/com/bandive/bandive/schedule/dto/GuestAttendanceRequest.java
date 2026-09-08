package com.bandive.bandive.schedule.dto;

import jakarta.validation.constraints.Size;

/**
 * 게스트를 일정에 추가/수정. 게스트는 항상 참석이므로 상태는 받지 않고, 그 일정에서 맡는 세션(악기 또는 "관객")만 받는다. null·빈 값이면 세션
 * 미지정.
 */
public record GuestAttendanceRequest(@Size(max = 30) String session) {
}
