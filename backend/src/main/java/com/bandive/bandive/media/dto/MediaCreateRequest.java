package com.bandive.bandive.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;

public record MediaCreateRequest(
		@NotBlank(message = "영상 URL 은 필수입니다") @Size(max = 500) @Pattern(regexp = "^https?://.+",
				message = "http(s):// 로 시작하는 URL 이어야 합니다") String externalUrl,
		@NotNull(message = "영상 종류(REHEARSAL/PERFORMANCE)는 필수입니다") MediaType type, MediaVisibility visibility,
		Long scheduleId) {
}
