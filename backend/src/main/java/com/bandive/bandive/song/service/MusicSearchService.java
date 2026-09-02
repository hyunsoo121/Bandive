package com.bandive.bandive.song.service;

import java.util.List;

import com.bandive.bandive.song.dto.TrackSearchResult;

/**
 * 외부 음원 검색. 지금은 {@link StubMusicSearchService}, Phase 5 에서 실제 API(Spotify 등) 구현체로 교체.
 */
public interface MusicSearchService {

	List<TrackSearchResult> search(String query);

}
