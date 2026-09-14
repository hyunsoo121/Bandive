package com.bandive.bandive.explore.dto;

import java.time.Instant;

import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaThumbnail;

/** 한 트랙에 대한 어느 밴드의 공개 합주 영상. */
public record ExploreVideoResponse(Long mediaId, Long bandId, String bandName, String url, String title,
		String thumbnailUrl, MediaPlatform platform, long likeCount, boolean likedByMe, Instant createdAt) {

	public static ExploreVideoResponse from(Media media, long likeCount, boolean likedByMe) {
		return new ExploreVideoResponse(media.getId(), media.getBand().getId(), media.getBand().getName(),
				media.getExternalUrl(), media.getTitle(),
				MediaThumbnail.of(media.getPlatform(), media.getExternalUrl()), media.getPlatform(), likeCount,
				likedByMe, media.getCreatedAt());
	}

}
