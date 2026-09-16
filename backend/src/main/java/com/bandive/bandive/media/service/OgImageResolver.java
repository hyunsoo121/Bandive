package com.bandive.bandive.media.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 외부 페이지의 Open Graph {@code og:image} 메타태그를 읽어온다 — 구글 포토 공유 링크처럼 URL 자체만으로는 썸네일 이미지를 계산할
 * 수 없고, 그 페이지가 링크 미리보기용으로 og:image 를 내려주는 경우에 쓴다. 구글이 페이지 구조를 바꾸면 깨질 수 있는 비공식 동작이라, 실패하면
 * 예외 없이 조용히 null 을 돌려준다. {@link MediaService} 가 등록/수정 시점에 한 번만 불러 결과를 저장해두고, 조회할 때마다 다시
 * 요청하지 않는다.
 */
@Service
public class OgImageResolver {

	private static final Pattern OG_IMAGE = Pattern
		.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

	private static final Pattern OG_IMAGE_REVERSED = Pattern
		.compile("<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);

	private final RestClient client;

	public OgImageResolver() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		this.client = RestClient.builder()
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; BandiveLinkPreview/1.0)")
			.build();
	}

	/** og:image 를 못 찾거나 요청이 실패하면 null. 절대 예외를 던지지 않는다. */
	public String resolve(String url) {
		try {
			String html = client.get().uri(url).retrieve().body(String.class);
			if (html == null) {
				return null;
			}
			Matcher m = OG_IMAGE.matcher(html);
			if (m.find()) {
				return m.group(1);
			}
			m = OG_IMAGE_REVERSED.matcher(html);
			return m.find() ? m.group(1) : null;
		}
		catch (RestClientException ex) {
			return null;
		}
	}

}
