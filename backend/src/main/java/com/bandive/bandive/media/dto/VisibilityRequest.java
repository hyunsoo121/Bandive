package com.bandive.bandive.media.dto;

import jakarta.validation.constraints.NotNull;

import com.bandive.bandive.media.MediaVisibility;

public record VisibilityRequest(
		@NotNull(message = "공개 범위(MEMBERS_ONLY/LINK_PUBLIC)는 필수입니다") MediaVisibility visibility) {
}
