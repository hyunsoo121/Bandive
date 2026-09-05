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
			  {"trackId":409076748,"trackName":"좋은 날","artistName":"아이유","kind":"song",
			   "artworkUrl100":"https://is1.mzstatic.com/x/100x100bb.jpg"},
			  {"trackId":1441164805,"trackName":"Yesterday","artistName":"The Beatles"},
			  {"trackId":null,"trackName":"이름만 있고 id 없음","artistName":"X"}
			]}""";

	private MockRestServiceServer server;

	private ItunesMusicSearchService service;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
		this.server = MockRestServiceServer.bindTo(builder).build();
		// 현지화 lookup 은 아래 별도 테스트에서 검증 — 여기선 꺼서(빈 값) /search 매핑에만 집중한다.
		this.service = new ItunesMusicSearchService(new MusicProperties("itunes", 8, "US", ""), builder.build(),
				JsonMapper.builder().build());
	}

	@Test
	void 검색결과를_TrackSearchResult_로_매핑한다() {
		this.server.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("term", "IU"))
			.andExpect(queryParam("entity", "song"))
			.andExpect(queryParam("limit", "8"))
			.andExpect(queryParam("country", "US"))
			.andRespond(withSuccess(RESPONSE_JSON, ITUNES_CT));

		// 응답 본문에 한글 트랙명이 섞여 있어도 그대로 매핑된다. id 없는 항목은 버린다.
		var results = this.service.search("IU");

		assertThat(results).containsExactly(
				new TrackSearchResult("409076748", "좋은 날", "아이유", "https://is1.mzstatic.com/x/600x600bb.jpg"),
				new TrackSearchResult("1441164805", "Yesterday", "The Beatles", null));
		this.server.verify();
	}

	@Test
	void US_로_검색하고_KR_lookup_으로_제목을_현지화한다() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
		MockRestServiceServer localServer = MockRestServiceServer.bindTo(builder).build();
		ItunesMusicSearchService localized = new ItunesMusicSearchService(new MusicProperties("itunes", 8, "US", "KR"),
				builder.build(), JsonMapper.builder().build());

		String usSearch = """
				{"resultCount":2,"results":[
				  {"trackId":409076748,"trackName":"Good Day","artistName":"IU",
				   "artworkUrl100":"https://is1.mzstatic.com/x/100x100bb.jpg"},
				  {"trackId":693573510,"trackName":"Yellow","artistName":"Coldplay"}
				]}""";
		// KR 스토어엔 IU 곡만 있고 Coldplay 는 없다 → Coldplay 는 US 값을 그대로 유지한다.
		String krLookup = """
				{"resultCount":1,"results":[
				  {"trackId":409076748,"trackName":"좋은 날","artistName":"아이유"}
				]}""";

		localServer.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andExpect(queryParam("country", "US"))
			.andRespond(withSuccess(usSearch, ITUNES_CT));
		localServer.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/lookup")))
			.andExpect(queryParam("country", "KR"))
			.andExpect(queryParam("id", Matchers.containsString("409076748")))
			.andRespond(withSuccess(krLookup, ITUNES_CT));

		assertThat(localized.search("IU good day")).containsExactly(
				new TrackSearchResult("409076748", "좋은 날", "아이유", "https://is1.mzstatic.com/x/600x600bb.jpg"),
				new TrackSearchResult("693573510", "Yellow", "Coldplay", null));
		localServer.verify();
	}

	@Test
	void 현지화_lookup_이_실패해도_검색_결과는_그대로_준다() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
		MockRestServiceServer localServer = MockRestServiceServer.bindTo(builder).build();
		ItunesMusicSearchService localized = new ItunesMusicSearchService(new MusicProperties("itunes", 8, "US", "KR"),
				builder.build(), JsonMapper.builder().build());

		localServer.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/search")))
			.andRespond(withSuccess(RESPONSE_JSON, ITUNES_CT));
		localServer.expect(requestTo(Matchers.startsWith("https://itunes.apple.com/lookup")))
			.andRespond(withServerError());

		assertThat(localized.search("IU")).containsExactly(
				new TrackSearchResult("409076748", "좋은 날", "아이유", "https://is1.mzstatic.com/x/600x600bb.jpg"),
				new TrackSearchResult("1441164805", "Yesterday", "The Beatles", null));
		localServer.verify();
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
