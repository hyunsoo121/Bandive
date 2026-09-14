package com.bandive.bandive.media.dto;

import java.time.Instant;

import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaThumbnail;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;

public record MediaResponse(Long id, Long bandId, Long scheduleId, Long songId, String songTitle, MediaType type,
		String externalUrl, String title, MediaPlatform platform, String thumbnailUrl, MediaVisibility visibility,
		Long uploadedByUserId, String uploadedByNickname, long likeCount, boolean likedByMe, Instant createdAt) {

	/** 좋아요 수가 필요 없는 임베드용(예: 일정 상세의 연결 영상 목록). likeCount=0, likedByMe=false. */
	public static MediaResponse from(Media media) {
		return from(media, 0L, false);
	}

	public static MediaResponse from(Media media, long likeCount, boolean likedByMe) {
		Long scheduleId = media.getSchedule() != null ? media.getSchedule().getId() : null;
		Long songId = media.getSong() != null ? media.getSong().getId() : null;
		String songTitle = media.getSong() != null ? media.getSong().getTitle() : null;
		return new MediaResponse(media.getId(), media.getBand().getId(), scheduleId, songId, songTitle, media.getType(),
				media.getExternalUrl(), media.getTitle(), media.getPlatform(),
				MediaThumbnail.of(media.getPlatform(), media.getExternalUrl()), media.getVisibility(),
				media.getUploadedBy().getId(), media.getUploadedBy().getNickname(), likeCount, likedByMe,
				media.getCreatedAt());
	}

}
