package com.bandive.bandive.guest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게스트 생성·이름 수정 공용. */
public record GuestRequest(@NotBlank @Size(max = 50) String name) {
}
