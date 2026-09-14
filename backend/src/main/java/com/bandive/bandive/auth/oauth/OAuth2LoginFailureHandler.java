package com.bandive.bandive.auth.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.bandive.bandive.auth.AuthProperties;

/**
 * 카카오 로그인 실패 → 이유를 쿼리로 붙여 프론트 실패 페이지로 리다이렉트.
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

	private final String failureRedirect;

	public OAuth2LoginFailureHandler(HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
			AuthProperties properties) {
		this.authorizationRequestRepository = authorizationRequestRepository;
		this.failureRedirect = properties.oauth2().failureRedirect();
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
		String reason = exception.getMessage() == null ? "login_failed"
				: URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
		String target = UriComponentsBuilder.fromUriString(failureRedirect)
			.queryParam("error", reason)
			.build()
			.toUriString();
		getRedirectStrategy().sendRedirect(request, response, target);
	}

}
