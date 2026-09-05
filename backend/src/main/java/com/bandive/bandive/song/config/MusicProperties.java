package com.bandive.bandive.song.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.music.*} — 외부 음원 검색 설정.
 *
 * @param provider {@code stub}(기본, 가짜 결과) 또는 {@code itunes}(iTunes Search API 실 검색)
 * @param limit 검색 결과 최대 개수
 * @param country 검색에 쓸 iTunes 스토어 국가 코드. ⚠️ {@code KR} 스토어는 Search API
 * {@code entity=song} 응답이 항상 비어 있어(2026-09-03 확인) {@code US} 를 기본값으로 둔다. US 카탈로그는 한글
 * 검색어("아이유 좋은날" → Good Day / IU)도 정상 매칭된다.
 * @param localizeCountry 검색 결과 제목/가수를 현지화할 스토어 국가 코드. US 로 찾은 trackId 를 이 스토어에 lookup 해
 * "Good Day" → "좋은 날" 로 바꾼다 (KR 은 검색은 죽었어도 lookup 은 살아 있음). 비우면 현지화 없이 검색 스토어 값을 그대로 쓴다.
 */
@ConfigurationProperties(prefix = "app.music")
public record MusicProperties(@DefaultValue("stub") String provider, @DefaultValue("8") int limit,
		@DefaultValue("US") String country, @DefaultValue("KR") String localizeCountry) {
}
