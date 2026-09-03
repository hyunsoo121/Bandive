package com.bandive.bandive.song.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.music.*} — 외부 음원 검색 설정.
 *
 * @param provider {@code stub}(기본, 가짜 결과) 또는 {@code itunes}(iTunes Search API 실 검색)
 * @param limit 검색 결과 최대 개수
 * @param country iTunes 스토어 국가 코드 (검색 결과 지역화)
 */
@ConfigurationProperties(prefix = "app.music")
public record MusicProperties(@DefaultValue("stub") String provider, @DefaultValue("8") int limit,
		@DefaultValue("KR") String country) {
}
