package com.bandive.bandive.explore.dto;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandVisibility;

/** 탐색 밴드 카드. 클릭 시 밴드 페이지로 이동해 거기서 관계·게이트를 처리한다. */
public record ExploreBandResponse(Long id, String name, String description, String logoUrl, BandVisibility visibility,
		long memberCount) {

	public static ExploreBandResponse from(Band band, long memberCount) {
		return new ExploreBandResponse(band.getId(), band.getName(), band.getDescription(), band.getLogoUrl(),
				band.getVisibility(), memberCount);
	}

}
