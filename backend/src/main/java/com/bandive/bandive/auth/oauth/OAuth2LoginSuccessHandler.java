package com.bandive.bandive.auth.oauth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.bandive.bandive.auth.AuthProperties;
import com.bandive.bandive.auth.SessionIssuer;

/**
 * 카카오 로그인 성공 → refresh 토큰을 Redis + httpOnly 쿠키에 심고 프론트로 리다이렉트. access 토큰은 여기서 주지 않는다.
 * 프론트가 복귀 후 {@code POST /api/auth/refresh} 로 첫 access 를 받아간다.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final SessionIssuer sessionIssuer;

	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

	private final String successRedirect;

	public OAuth2LoginSuccessHandler(SessionIssuer sessionIssuer,
			HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository, AuthProperties properties) {
		this.sessionIssuer = sessionIssuer;
		this.authorizationRequestRepository = authorizationRequestRepository;
		this.successRedirect = properties.oauth2().successRedirect();
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		Long userId = ((KakaoOAuth2User) authentication.getPrincipal()).getUserId();

		sessionIssuer.openRefreshSession(userId, response);

		authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
		clearAuthenticationAttributes(request);
		getRedirectStrategy().sendRedirect(request, response, successRedirect);
	}

}
