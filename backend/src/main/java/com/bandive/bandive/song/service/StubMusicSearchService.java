package com.bandive.bandive.song.service;

import java.util.List;

import com.bandive.bandive.song.dto.TrackSearchResult;

/**
 * 실제 음원 API 를 안 쓸 때의 기본 구현. 쿼리를 echo 한 가짜 결과를 준다 (프론트 "곡 검색" 모달 연동/테스트용).
 * {@code app.music.provider=itunes} 로 바꾸면 {@link ItunesMusicSearchService} 가 대신 뜬다
 * ({@code MusicSearchConfig} 에서 빈 선택).
 */
public class StubMusicSearchService implements MusicSearchService {

	@Override
	public List<TrackSearchResult> search(String query) {
		if (query == null || query.isBlank()) {
			return List.of();
		}
		String q = query.trim();
		return List.of(new TrackSearchResult("stub:" + q + ":1", q + " (샘플 트랙 1)", "샘플 아티스트 A"),
				new TrackSearchResult("stub:" + q + ":2", q + " (샘플 트랙 2)", "샘플 아티스트 B"),
				new TrackSearchResult("stub:" + q + ":3", q + " (커버)", "샘플 밴드"));
	}

}
