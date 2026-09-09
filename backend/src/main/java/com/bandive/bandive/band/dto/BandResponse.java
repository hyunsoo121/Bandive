package com.bandive.bandive.band.dto;

import java.time.Instant;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.band.MyRelation;
import com.bandive.bandive.member.BandRole;

/**
 * 밴드 응답 (표지 정보). {@code role} 은 "현재 사용자의 이 밴드에서의 역할" 이며 역할을 알 수 있는 문맥에서만 채워지고 그 외 null.
 * {@code myRelation} 은 항상 채워진다 (비로그인이면 {@code NONE}).
 */
public record BandResponse(Long id, String name, String description, String logoUrl, String bannerUrl, long memberCount,
		BandVisibility visibility, BandRole role, MyRelation myRelation, Instant createdAt) {

	public static BandResponse from(Band band, long memberCount) {
		return from(band, memberCount, null, MyRelation.NONE);
	}

	/** role 이 있으면 멤버라는 뜻 → myRelation 은 MEMBER. */
	public static BandResponse from(Band band, long memberCount, BandRole role) {
		return from(band, memberCount, role, role != null ? MyRelation.MEMBER : MyRelation.NONE);
	}

	public static BandResponse from(Band band, long memberCount, BandRole role, MyRelation myRelation) {
		return new BandResponse(band.getId(), band.getName(), band.getDescription(), band.getLogoUrl(),
				band.getBannerUrl(), memberCount, band.getVisibility(), role, myRelation, band.getCreatedAt());
	}

}
