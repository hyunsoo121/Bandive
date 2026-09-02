package com.bandive.bandive.media.dto;

import java.time.Instant;

import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;

public record MediaResponse(Long id, Long bandId, Long scheduleId, MediaType type, String externalUrl,
		MediaPlatform platform, MediaVisibility visibility, Long uploadedByUserId, String uploadedByNickname,
		Instant createdAt) {

	public static MediaResponse from(Media media) {
		Long scheduleId = media.getSchedule() != null ? media.getSchedule().getId() : null;
		return new MediaResponse(media.getId(), media.getBand().getId(), scheduleId, media.getType(),
				media.getExternalUrl(), media.getPlatform(), media.getVisibility(), media.getUploadedBy().getId(),
				media.getUploadedBy().getNickname(), media.getCreatedAt());
	}

}
