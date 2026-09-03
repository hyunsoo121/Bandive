package com.bandive.bandive.song.config;

import java.net.http.HttpClient;
import java.time.Duration;

import tools.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.bandive.bandive.song.service.ItunesMusicSearchService;
import com.bandive.bandive.song.service.MusicSearchService;
import com.bandive.bandive.song.service.StubMusicSearchService;

/**
 * 음원 검색 구현체 선택. {@code app.music.provider=itunes} 면 {@link ItunesMusicSearchService}, 그
 * 외(미설정 포함) 모든 값은 {@link StubMusicSearchService}.
 */
@Configuration
@EnableConfigurationProperties(MusicProperties.class)
public class MusicSearchConfig {

	@Bean
	@ConditionalOnProperty(prefix = "app.music", name = "provider", havingValue = "itunes")
	MusicSearchService itunesMusicSearchService(MusicProperties properties, ObjectMapper objectMapper) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		RestClient client = RestClient.builder()
			.baseUrl("https://itunes.apple.com")
			.requestFactory(requestFactory)
			.build();
		return new ItunesMusicSearchService(properties, client, objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean(MusicSearchService.class)
	MusicSearchService stubMusicSearchService() {
		return new StubMusicSearchService();
	}

}
