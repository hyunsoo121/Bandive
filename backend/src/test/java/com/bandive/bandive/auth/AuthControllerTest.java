package com.bandive.bandive.auth;

import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.auth.jwt.RefreshToken;
import com.bandive.bandive.support.IntegrationTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerTest extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private RefreshTokenStore refreshTokenStore;

	@Autowired
	private UserRepository users;

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = users.save(User.builder().kakaoId("kakao-" + UUID.randomUUID()).nickname("테스터").build()).getId();
	}

	@AfterEach
	void tearDown() {
		refreshTokenStore.delete(userId);
		users.deleteById(userId);
	}

	private Cookie issueRefreshCookie() {
		RefreshToken refresh = jwtProvider.createRefreshToken(userId);
		refreshTokenStore.save(userId, refresh.jti(), refresh.ttl());
		return new Cookie(CookieUtils.REFRESH_COOKIE, refresh.value());
	}

	@Test
	void refresh_는_새_access_와_회전된_refresh_쿠키를_준다() throws Exception {
		mvc.perform(post("/api/auth/refresh").cookie(issueRefreshCookie()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.expiresIn").value(1800))
			.andExpect(cookie().exists(CookieUtils.REFRESH_COOKIE))
			.andExpect(cookie().httpOnly(CookieUtils.REFRESH_COOKIE, true));
	}

	@Test
	void refresh_쿠키가_없으면_401() throws Exception {
		mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
	}

	@Test
	void 폐기된_refresh_는_401() throws Exception {
		Cookie cookie = issueRefreshCookie();
		refreshTokenStore.delete(userId);

		mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
	}

	@Test
	void 회전_후_이전_refresh_는_재사용_불가() throws Exception {
		Cookie old = issueRefreshCookie();
		mvc.perform(post("/api/auth/refresh").cookie(old)).andExpect(status().isOk());

		mvc.perform(post("/api/auth/refresh").cookie(old)).andExpect(status().isUnauthorized());
	}

	@Test
	void logout_은_세션을_지우고_쿠키를_만료시킨다() throws Exception {
		Cookie cookie = issueRefreshCookie();

		mvc.perform(post("/api/auth/logout").cookie(cookie))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge(CookieUtils.REFRESH_COOKIE, 0));

		assertThat(refreshTokenStore.matches(userId, jwtProvider.parseRefresh(cookie.getValue()).getId())).isFalse();
	}

	@Test
	void me_는_유효한_access_로_내_정보를_준다() throws Exception {
		String access = jwtProvider.createAccessToken(userId);

		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.nickname").value("테스터"));
	}

}
