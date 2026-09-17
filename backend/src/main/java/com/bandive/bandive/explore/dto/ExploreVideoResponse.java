package com.bandive.bandive.explore.dto;

import java.time.Instant;

import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaThumbnail;

/** 한 트랙에 대한 어느 밴드의 공개 합주 영상. */
public record ExploreVideoResponse(Long mediaId, Long bandId, String bandName, String url, String title,
		String thumbnailUrl, MediaPlatform platform, long likeCount, boolean likedByMe, Instant createdAt) {

	public static ExploreVideoResponse from(Media media, long likeCount, boolean likedByMe) {
		// 구글 포토처럼 등록 시점에 fetch 해 저장해둔 값이 있으면 그걸 쓰고, 없으면(유튜브·드라이브)
		// URL 에서 즉석 계산한다 — MediaResponse.from() 과 동일한 우선순위.
		String thumbnailUrl = media.getThumbnailUrl() != null ? media.getThumbnailUrl()
				: MediaThumbnail.of(media.getPlatform(), media.getExternalUrl());
		return new ExploreVideoResponse(media.getId(), media.getBand().getId(), media.getBand().getName(),
				media.getExternalUrl(), media.getTitle(), thumbnailUrl, media.getPlatform(), likeCount, likedByMe,
				media.getCreatedAt());
	}

}
