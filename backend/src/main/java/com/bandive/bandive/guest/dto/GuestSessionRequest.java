package com.bandive.bandive.guest.dto;

import jakarta.validation.constraints.Size;

/** 게스트 세션 설정. null·빈 값이면 세션 미지정으로 지운다. */
public record GuestSessionRequest(@Size(max = 30) String session) {
}
