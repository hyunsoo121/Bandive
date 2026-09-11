package com.bandive.bandive.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 내 정보 수정 — 닉네임(필수) + 한 줄 소개(선택, 빈 값이면 지운다). */
public record ProfileUpdateRequest(@NotBlank(message = "닉네임은 필수입니다") @Size(max = 50) String nickname,
		@Size(max = 100, message = "한 줄 소개는 100자 이내여야 합니다") String bio) {
}
