package com.bandive.bandive.explore.dto;

/** 탐색 곡(트랙) 한 건. {@code externalTrackId} 로 여러 밴드의 합주 영상이 묶인다. */
public record ExploreTrackResponse(String externalTrackId, String title, String artist, String artworkUrl,
		long bandCount, long videoCount) {

	public static ExploreTrackResponse of(Object[] row) {
		return new ExploreTrackResponse((String) row[0], (String) row[1], (String) row[2], (String) row[3],
				((Number) row[4]).longValue(), ((Number) row[5]).longValue());
	}

}
