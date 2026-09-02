package com.bandive.bandive.band.dto;

import java.time.Instant;

import com.bandive.bandive.band.Band;

public record BandResponse(Long id, String name, String description, String logoUrl, String bannerUrl, long memberCount,
		Instant createdAt) {

	public static BandResponse from(Band band, long memberCount) {
		return new BandResponse(band.getId(), band.getName(), band.getDescription(), band.getLogoUrl(),
				band.getBannerUrl(), memberCount, band.getCreatedAt());
	}

}
