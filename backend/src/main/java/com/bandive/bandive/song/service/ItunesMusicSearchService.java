package com.bandive.bandive.song.service;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bandive.bandive.song.config.MusicProperties;
import com.bandive.bandive.song.dto.TrackSearchResult;

/**
 * Apple iTunes Search API 로 트랙을 검색한다. 인증·API 키가 필요 없다.
 * <p>
 * iTunes 는 응답을 {@code Content-Type: text/javascript} 로 주므로 (본문은 JSON) 문자열로 받아 직접 파싱한다. 외부
 * 장애(타임아웃·4xx·5xx·깨진 본문)는 삼켜서 빈 목록을 돌려준다 — "곡 검색" 모달이 500 대신 결과 없음으로 보이는 편이 낫다.
 */
public class ItunesMusicSearchService implements MusicSearchService {

	private static final Logger log = LoggerFactory.getLogger(ItunesMusicSearchService.class);

	private final RestClient client;

	private final ObjectMapper objectMapper;

	private final int limit;

	private final String country;

	public ItunesMusicSearchService(MusicProperties properties, RestClient client, ObjectMapper objectMapper) {
		this.client = client;
		this.objectMapper = objectMapper;
		this.limit = Math.max(1, properties.limit());
		this.country = StringUtils.hasText(properties.country()) ? properties.country() : "US";
	}

	@Override
	public List<TrackSearchResult> search(String query) {
		if (!StringUtils.hasText(query)) {
			return List.of();
		}
		try {
			String body = client.get()
				.uri(builder -> builder.path("/search")
					.queryParam("term", query.trim())
					.queryParam("entity", "song")
					.queryParam("limit", limit)
					.queryParam("country", country)
					.build())
				.retrieve()
				.body(String.class);
			if (!StringUtils.hasText(body)) {
				return List.of();
			}
			ItunesResponse response = objectMapper.readValue(body, ItunesResponse.class);
			if (response.results() == null) {
				return List.of();
			}
			return response.results()
				.stream()
				.filter(Objects::nonNull)
				.map(ItunesMusicSearchService::toResult)
				.filter(Objects::nonNull)
				.toList();
		}
		catch (RestClientException | JacksonException ex) {
			log.warn("iTunes 검색 실패 (query={}): {}", query, ex.getMessage());
			return List.of();
		}
	}

	private static TrackSearchResult toResult(ItunesTrack track) {
		if (track.trackId() == null || !StringUtils.hasText(track.trackName())) {
			return null;
		}
		return new TrackSearchResult(String.valueOf(track.trackId()), track.trackName(),
				track.artistName() == null ? "" : track.artistName());
	}

	record ItunesResponse(@JsonProperty("results") List<ItunesTrack> results) {
	}

	record ItunesTrack(@JsonProperty("trackId") Long trackId, @JsonProperty("trackName") String trackName,
			@JsonProperty("artistName") String artistName) {
	}

}
