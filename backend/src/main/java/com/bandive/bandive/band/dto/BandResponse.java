package com.bandive.bandive.band.dto;

import java.time.Instant;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.member.BandRole;

/**
 * 밴드 응답. {@code role} 은 "현재 사용자의 이 밴드에서의 역할" 이며, 역할을 알 수 있는 문맥
 * ({@code GET /api/bands/my}, 밴드 생성, 초대 가입)에서만 채워지고 그 외에는 null.
 */
public record BandResponse(Long id, String name, String description, String logoUrl, String bannerUrl, long memberCount,
		BandRole role, Instant createdAt) {

	public static BandResponse from(Band band, long memberCount) {
		return from(band, memberCount, null);
	}

	public static BandResponse from(Band band, long memberCount, BandRole role) {
		return new BandResponse(band.getId(), band.getName(), band.getDescription(), band.getLogoUrl(),
				band.getBannerUrl(), memberCount, role, band.getCreatedAt());
	}

}
