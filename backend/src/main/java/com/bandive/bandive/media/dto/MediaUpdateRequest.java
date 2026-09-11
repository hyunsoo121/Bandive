package com.bandive.bandive.media.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;

/**
 * 영상 부분 수정. 모든 필드 선택 — null 인 필드는 기존 값을 유지한다. {@code scheduleId}·{@code songId} 는 값이 있으면
 * 재연결, null 이면 유지(연결 해제는 지원 안 함).
 */
public record MediaUpdateRequest(
		@Size(max = 500) @Pattern(regexp = "^https?://.+",
				message = "http(s):// 로 시작하는 URL 이어야 합니다") String externalUrl,
		MediaType type, MediaVisibility visibility, @Size(max = 200) String title, Long scheduleId, Long songId) {
}
