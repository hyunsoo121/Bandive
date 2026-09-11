package com.bandive.bandive.band.dto;

import jakarta.validation.constraints.NotNull;

import com.bandive.bandive.band.BandVisibility;

/** 밴드 공개범위 변경 (관리자). */
public record BandVisibilityRequest(@NotNull(message = "공개범위는 필수입니다") BandVisibility visibility) {
}
