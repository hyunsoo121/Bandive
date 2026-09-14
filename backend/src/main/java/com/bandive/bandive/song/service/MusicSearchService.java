package com.bandive.bandive.song.service;

import java.util.List;

import com.bandive.bandive.song.dto.TrackSearchResult;

/**
 * 외부 음원 검색. 기본은 {@link StubMusicSearchService}, {@code app.music.provider=itunes} 면
 * {@link ItunesMusicSearchService}.
 */
public interface MusicSearchService {

	List<TrackSearchResult> search(String query);

}
