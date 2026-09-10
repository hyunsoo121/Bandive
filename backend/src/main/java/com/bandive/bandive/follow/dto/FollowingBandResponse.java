package com.bandive.bandive.follow.dto;

import java.time.Instant;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.follow.FollowStatus;

/** 내가 팔로우한 밴드 한 건 (GET /api/me/following). */
public record FollowingBandResponse(Long bandId, String name, String description, String logoUrl,
		BandVisibility visibility, long memberCount, FollowStatus status, Instant requestedAt) {

	/** {@code row = [Band, FollowStatus, Instant createdAt, long memberCount]} */
	public static FollowingBandResponse of(Object[] row) {
		Band band = (Band) row[0];
		return new FollowingBandResponse(band.getId(), band.getName(), band.getDescription(), band.getLogoUrl(),
				band.getVisibility(), ((Number) row[3]).longValue(), (FollowStatus) row[1], (Instant) row[2]);
	}

}
