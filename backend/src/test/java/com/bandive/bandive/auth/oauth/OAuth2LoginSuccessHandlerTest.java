package com.bandive.bandive.auth.oauth;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.CookieUtils;
import com.bandive.bandive.auth.RefreshTokenStore;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.support.IntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginSuccessHandlerTest extends IntegrationTest {

	@Autowired
	private OAuth2LoginSuccessHandler handler;

	@Autowired
	private RefreshTokenStore refreshTokenStore;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 성공하면_refresh_를_저장하고_쿠키를_심고_프론트로_리다이렉트한다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		var principal = new KakaoOAuth2User(4242L, Map.of("id", 4242L));
		var authentication = new TestingAuthenticationToken(principal, null);

		handler.onAuthenticationSuccess(request, response, authentication);

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/oauth/success");

		String setCookie = response.getHeader("Set-Cookie");
		assertThat(setCookie).contains(CookieUtils.REFRESH_COOKIE).contains("HttpOnly").contains("SameSite=Strict");

		String refreshValue = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
		String jti = jwtProvider.parseRefresh(refreshValue).getId();
		assertThat(refreshTokenStore.matches(4242L, jti)).isTrue();
	}

}
