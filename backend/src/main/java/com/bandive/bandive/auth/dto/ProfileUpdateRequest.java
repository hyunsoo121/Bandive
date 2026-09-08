package com.bandive.bandive.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 내 정보 수정 — 현재는 닉네임만. */
public record ProfileUpdateRequest(@NotBlank(message = "닉네임은 필수입니다") @Size(max = 50) String nickname) {
}
