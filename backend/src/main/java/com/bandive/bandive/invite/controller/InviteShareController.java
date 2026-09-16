package com.bandive.bandive.invite.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.bandive.bandive.common.config.FrontendProperties;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.invite.dto.InvitePreviewResponse;
import com.bandive.bandive.invite.service.InviteService;

/**
 * 초대 링크 공유용 (카카오톡 등 링크 미리보기). 실제 참여 화면은 SPA 의 {@code /join/{code}} 지만, 메신저 크롤러는 자바스크립트를 안
 * 돌려서 SPA 가 OG 태그를 못 채운다. 그래서 공유 URL({@code /invite/{code}}, Caddy 가 이 경로만 백엔드로 보냄)은 이
 * 컨트롤러가 OG 태그 박은 정적 HTML 로 응답하고, 사람이 열면 그 안의 meta-refresh/스크립트로 {@code /join/{code}} 로
 * 넘어간다.
 */
@RestController
public class InviteShareController {

	private final InviteService inviteService;

	private final String frontendBaseUrl;

	public InviteShareController(InviteService inviteService, FrontendProperties frontendProperties) {
		this.inviteService = inviteService;
		String baseUrl = frontendProperties.baseUrl();
		this.frontendBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	@GetMapping(value = "/invite/{code}", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> share(@PathVariable String code) {
		String redirectUrl = frontendBaseUrl + "/join/" + HtmlUtils.htmlEscape(code);

		String title = "밴디브 초대";
		String description = "밴드에 참여해보세요.";
		String imageTag = "";
		try {
			InvitePreviewResponse preview = inviteService.preview(code);
			title = HtmlUtils.htmlEscape(preview.bandName()) + " 초대";
			description = HtmlUtils.htmlEscape(preview.bandName()) + "에 참여해보세요 · 멤버 " + preview.memberCount() + "명";
			if (preview.logoUrl() != null) {
				imageTag = "<meta property=\"og:image\" content=\"" + HtmlUtils.htmlEscape(preview.logoUrl()) + "\">\n";
			}
		}
		catch (NotFoundException ignored) {
			// 유효하지 않은 코드 — 기본 문구로 안내만 하고 그대로 SPA 로 넘긴다 (SPA 쪽에서 가입 시도 시 404 처리).
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

	/** JS 문자열 리터럴 이스케이프 (redirectUrl 은 baseUrl 설정값 + 영숫자 코드라 사실상 안전하지만 방어적으로). */
	private static String jsStringLiteral(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

}
