package com.bandive.bandive.explore.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.bandive.bandive.common.config.FrontendProperties;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.explore.dto.ExploreBandDetailResponse;
import com.bandive.bandive.explore.dto.ExploreBandResponse;
import com.bandive.bandive.explore.service.ExploreService;

/**
 * 밴드 구경 링크 공유용 (카카오톡 등 링크 미리보기). {@code InviteShareController} 와 같은 이유 — 메신저 크롤러는 자바스크립트를
 * 안 돌려서 SPA 가 OG 태그를 못 채운다. 공유 URL({@code /band/{id}}, Caddy 가 이 경로만 백엔드로 보냄)은 이 컨트롤러가 OG
 * 태그 박은 정적 HTML 로 응답하고, 사람이 열면 실제 구경 화면인 {@code /explore/bands/{id}} 로 넘어간다. 비공개 밴드거나 없는
 * 밴드면 기본 문구로 그대로 SPA 에 넘긴다.
 */
@RestController
public class BandShareController {

	private final ExploreService exploreService;

	private final String frontendBaseUrl;

	public BandShareController(ExploreService exploreService, FrontendProperties frontendProperties) {
		this.exploreService = exploreService;
		String baseUrl = frontendProperties.baseUrl();
		this.frontendBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	@GetMapping(value = "/band/{bandId}", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> share(@PathVariable Long bandId) {
		String redirectUrl = frontendBaseUrl + "/explore/bands/" + bandId;

		String title = "밴디브";
		String description = "밴드를 구경해보세요.";
		String imageTag = "";
		try {
			ExploreBandDetailResponse detail = exploreService.bandDetail(bandId, null);
			ExploreBandResponse card = detail.band();
			title = HtmlUtils.htmlEscape(card.name());
			description = (card.description() != null ? HtmlUtils.htmlEscape(card.description()) + " · " : "") + "멤버 "
					+ card.memberCount() + "명";
			if (card.logoUrl() != null) {
				imageTag = "<meta property=\"og:image\" content=\"" + HtmlUtils.htmlEscape(card.logoUrl()) + "\">\n";
			}
		}
		catch (NotFoundException ignored) {
			// 비공개거나 없는 밴드 — 기본 문구로 안내만 하고 그대로 SPA 로 넘긴다.
		}

		String html = """
				<!doctype html>
				<html lang="ko">
				<head>
				<meta charset="UTF-8">
				<title>%s</title>
				<meta property="og:type" content="website">
				<meta property="og:title" content="%s">
				<meta property="og:description" content="%s">
				%s<meta http-equiv="refresh" content="0; url=%s">
				<script>location.replace(%s);</script>
				</head>
				<body>
				<p>이동 중입니다… <a href="%s">여기를 눌러주세요</a></p>
				</body>
				</html>
				""".formatted(title, title, description, imageTag, redirectUrl, jsStringLiteral(redirectUrl),
				redirectUrl);

		return ResponseEntity.ok().body(html);
	}

	private static String jsStringLiteral(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

}
