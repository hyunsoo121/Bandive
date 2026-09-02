package com.bandive.bandive.song.dto;

/**
 * 외부 음원 검색 결과 1건. {@code externalTrackId} 를 곡 추가 시 그대로 넘긴다.
 */
public record TrackSearchResult(String externalTrackId, String title, String artist) {
}
