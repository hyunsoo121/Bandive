package com.bandive.bandive.song.dto;

/**
 * 외부 음원 검색 결과 1건. {@code externalTrackId} 와 {@code artworkUrl} 을 곡 추가 시 그대로 넘긴다.
 *
 * @param artworkUrl 앨범 커버 이미지 URL (없으면 null)
 */
public record TrackSearchResult(String externalTrackId, String title, String artist, String artworkUrl) {
}
