package com.bandive.bandive.song.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
 * <p>
 * 검색은 {@code country}(기본 US) 스토어로 한다 — KR 스토어는 {@code entity=song} 검색이 죽어 있다. 대신 찾은
 * trackId 를 {@code localizeCountry}(기본 KR) 스토어에 lookup 한 번 더 던져 현지화된 제목/가수("Good Day" →
 * "좋은 날")로 바꾼다. lookup 이 실패하거나 그 스토어에 없는 곡은 검색 스토어 값을 그대로 쓴다.
 */
public class ItunesMusicSearchService implements MusicSearchService {

	private static final Logger log = LoggerFactory.getLogger(ItunesMusicSearchService.class);

	private final RestClient client;

	private final ObjectMapper objectMapper;

	private final int limit;

	private final String country;

	private final String localizeCountry;

	public ItunesMusicSearchService(MusicProperties properties, RestClient client, ObjectMapper objectMapper) {
		this.client = client;
		this.objectMapper = objectMapper;
		this.limit = Math.max(1, properties.limit());
		this.country = StringUtils.hasText(properties.country()) ? properties.country().trim() : "US";
		this.localizeCountry = StringUtils.hasText(properties.localizeCountry()) ? properties.localizeCountry().trim()
				: null;
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
			List<ItunesTrack> tracks = response.results()
				.stream()
				.filter(Objects::nonNull)
				.filter(track -> track.trackId() != null && StringUtils.hasText(track.trackName()))
				.toList();
			Map<Long, ItunesTrack> localized = localize(tracks);
			return tracks.stream().map(track -> toResult(track, localized.get(track.trackId()))).toList();
		}
		catch (RestClientException | JacksonException ex) {
			log.warn("iTunes 검색 실패 (query={}): {}", query, ex.getMessage());
			return List.of();
		}
	}

	/**
	 * 검색 결과 trackId 들을 {@code localizeCountry} 스토어에 한 번에 lookup 해서 (trackId → 현지화 트랙) 맵을
	 * 만든다. 현지화가 꺼져 있거나(빈 값), 검색 스토어와 같거나, lookup 이 실패하면 빈 맵 — 호출부는 검색 스토어 값을 쓴다.
	 */
	private Map<Long, ItunesTrack> localize(List<ItunesTrack> tracks) {
		if (localizeCountry == null || localizeCountry.equalsIgnoreCase(country) || tracks.isEmpty()) {
			return Map.of();
		}
		String ids = tracks.stream().map(track -> String.valueOf(track.trackId())).collect(Collectors.joining(","));
		try {
			String body = client.get()
				.uri(builder -> builder.path("/lookup")
					.queryParam("id", ids)
					.queryParam("country", localizeCountry)
					.build())
				.retrieve()
				.body(String.class);
			if (!StringUtils.hasText(body)) {
				return Map.of();
			}
			ItunesResponse response = objectMapper.readValue(body, ItunesResponse.class);
			if (response.results() == null) {
				return Map.of();
			}
			Map<Long, ItunesTrack> byId = new HashMap<>();
			for (ItunesTrack track : response.results()) {
				if (track != null && track.trackId() != null && StringUtils.hasText(track.trackName())) {
					byId.putIfAbsent(track.trackId(), track);
				}
			}
			return byId;
		}
		catch (RestClientException | JacksonException ex) {
			log.warn("iTunes 현지화 lookup 실패 (country={}): {}", localizeCountry, ex.getMessage());
			return Map.of();
		}
	}

	/** 아트워크·trackId 는 검색 스토어 기준, 제목/가수는 현지화 스토어 값이 있으면 우선. */
	private static TrackSearchResult toResult(ItunesTrack base, ItunesTrack localized) {
		ItunesTrack naming = (localized != null) ? localized : base;
		return new TrackSearchResult(String.valueOf(base.trackId()), naming.trackName(),
				naming.artistName() == null ? "" : naming.artistName(), largeArtwork(base.artworkUrl100()));
	}

	/** iTunes 는 100x100 썸네일을 준다. URL 의 크기 세그먼트를 키워 더 큰 이미지를 쓴다. */
	private static String largeArtwork(String url) {
		return StringUtils.hasText(url) ? url.replace("100x100bb", "600x600bb") : null;
	}

	record ItunesResponse(@JsonProperty("results") List<ItunesTrack> results) {
	}

	record ItunesTrack(@JsonProperty("trackId") Long trackId, @JsonProperty("trackName") String trackName,
			@JsonProperty("artistName") String artistName, @JsonProperty("artworkUrl100") String artworkUrl100) {
	}

}
