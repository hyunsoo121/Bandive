package com.bandive.bandive.song.service;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bandive.bandive.song.config.MusicProperties;
import com.bandive.bandive.song.dto.TrackSearchResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItunesMusicSearchServiceTest {

	// iTunes 는 본문은 JSON 이지만 Content-Type 을 text/javascript 로 준다.
	private static final MediaType ITUNES_CT = MediaType.parseMediaType("text/javascript;charset=utf-8");

	private static final String RESPONSE_JSON = """
			{"resultCount":3,"results":[
			  {"trackId":409076748,"trackName":"좋은 날","artistName":"아이유","kind":"song"},
			  {"trackId":1441164805,"trackName":"Yesterday","artistName":"The Beatles"},
			  {"trackId":null,"trackName":"이름만 있고 id 없음","artistName":"X"}
			]}""";

	private MockRestServiceServer server;

	private ItunesMusicSearchService service;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
		this.server = MockRestServiceServer.bindTo(builder).build();
		this.service = new ItunesMusicSearchService(new MusicProperties("itunes", 8, "KR"), builder.build(),
				JsonMapper.builder().build());
	}

	@Test
	void 검색결과를_TrackSearchResult_로_매핑한다() {
		this.server.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("term", "IU"))
			.andExpect(queryParam("entity", "song"))
			.andExpect(queryParam("limit", "8"))
			.andExpect(queryParam("country", "KR"))
			.andRespond(withSuccess(RESPONSE_JSON, ITUNES_CT));

		// 응답 본문에 한글 트랙명이 섞여 있어도 그대로 매핑된다. id 없는 항목은 버린다.
		var results = this.service.search("IU");

		assertThat(results).containsExactly(new TrackSearchResult("409076748", "좋은 날", "아이유"),
				new TrackSearchResult("1441164805", "Yesterday", "The Beatles"));
		this.server.verify();
	}

	@Test
	void 빈_쿼리는_외부_호출_없이_빈_결과() {
		assertThat(this.service.search("  ")).isEmpty();
		assertThat(this.service.search(null)).isEmpty();

		this.server.verify();
	}

	@Test
	void 검색이_5xx_면_빈_결과로_삼킨다() {
		this.server.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andRespond(withServerError());

		assertThat(this.service.search("Yesterday")).isEmpty();
	}

	@Test
	void 본문이_깨진_JSON_이면_빈_결과로_삼킨다() {
		this.server.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andRespond(withSuccess("<!DOCTYPE html><html>대충 에러 페이지</html>", ITUNES_CT));

		assertThat(this.service.search("Yesterday")).isEmpty();
	}

}
