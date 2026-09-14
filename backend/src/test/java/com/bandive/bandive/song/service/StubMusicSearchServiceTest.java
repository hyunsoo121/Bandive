package com.bandive.bandive.song.service;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.song.dto.TrackSearchResult;

import static org.assertj.core.api.Assertions.assertThat;

class StubMusicSearchServiceTest {

	private final StubMusicSearchService service = new StubMusicSearchService();

	@Test
	void 쿼리를_echo_한_가짜_결과를_준다() {
		var results = service.search("Yesterday");

		assertThat(results).hasSize(3);
		assertThat(results).extracting(TrackSearchResult::title).allMatch(title -> title.contains("Yesterday"));
		assertThat(results).extracting(TrackSearchResult::externalTrackId).allMatch(id -> id.startsWith("stub:"));
	}

	@Test
	void 빈_쿼리는_빈_결과() {
		assertThat(service.search("  ")).isEmpty();
		assertThat(service.search(null)).isEmpty();
	}

}
