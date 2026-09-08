package com.bandive.bandive.song.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.song.SongSourceType;

/**
 * 곡 추가. SEARCH 면 {@code externalTrackId} 필수(서비스에서 검사). {@code sessions} 로 SongPart 슬롯을
 * 만든다.
 * <p>
 * {@code artworkUrl}·{@code referenceVideoUrl} 은 프론트에서 그대로
 * {@code <img src>}·{@code <a href>} 로 렌더되므로 {@code http(s)://} 로 시작하는 값 (또는 빈 값) 만 허용한다
 * — {@code javascript:} 등 차단.
 */
public record SongCreateRequest(@NotBlank(message = "곡 제목은 필수입니다") @Size(max = 200) String title,
		@Size(max = 200) String artist, @NotNull(message = "등록 방식(SEARCH/MANUAL)은 필수입니다") SongSourceType sourceType,
		@Size(max = 100) String externalTrackId,
		@Size(max = 500) @Pattern(regexp = "^(https?://.+)?$",
				message = "앨범 이미지 주소는 http(s):// 로 시작해야 합니다") String artworkUrl,
		@Size(max = 2000, message = "메모는 2000자 이내여야 합니다") String memo,
		@Size(max = 500) @Pattern(regexp = "^(https?://.+)?$",
				message = "참고 영상 주소는 http(s):// 로 시작해야 합니다") String referenceVideoUrl,
		@Valid List<SessionSlot> sessions) {

	/** 악기별 필요 인원. instrument x count 만큼 SongPart 가 생성된다. */
	public record SessionSlot(@NotBlank(message = "악기 이름이 비어 있습니다") @Size(max = 20) String instrument,
			@Min(value = 1, message = "인원은 1 이상이어야 합니다") @Max(value = 10, message = "인원은 10 이하여야 합니다") int count) {
	}

}
