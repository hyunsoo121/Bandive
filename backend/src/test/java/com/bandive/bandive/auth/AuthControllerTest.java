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
		userId = users.save(User.ofKakao("kakao-" + UUID.randomUUID(), "테스터")).getId();
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

	@Test
	void 이메일_회원가입은_201_과_access_토큰_그리고_refresh_쿠키() throws Exception {
		String email = "signup-" + UUID.randomUUID() + "@example.com";

		mvc.perform(post("/api/auth/signup").contentType("application/json")
			.content("{\"email\":\"" + email + "\",\"password\":\"pass1234\",\"nickname\":\"이메일가입자\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(cookie().exists(CookieUtils.REFRESH_COOKIE));

		User created = users.findByEmail(email).orElseThrow();
		assertThat(created.getPasswordHash()).isNotBlank().isNotEqualTo("pass1234");
		users.deleteById(created.getId());
	}

	@Test
	void 약한_비밀번호는_400() throws Exception {
		mvc.perform(post("/api/auth/signup").contentType("application/json")
			.content("{\"email\":\"weak@example.com\",\"password\":\"onlyletters\",\"nickname\":\"약비번\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 이미_가입된_이메일이면_409() throws Exception {
		String email = "dup-" + UUID.randomUUID() + "@example.com";
		String body = "{\"email\":\"" + email + "\",\"password\":\"pass1234\",\"nickname\":\"먼저\"}";
		mvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
			.andExpect(status().isCreated());

		mvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

		users.findByEmail(email).ifPresent(u -> users.deleteById(u.getId()));
	}

	@Test
	void 이메일_로그인_성공과_틀린_비밀번호_401() throws Exception {
		String email = "login-" + UUID.randomUUID() + "@example.com";
		mvc.perform(post("/api/auth/signup").contentType("application/json")
			.content("{\"email\":\"" + email + "\",\"password\":\"pass1234\",\"nickname\":\"로그인유저\"}"))
			.andExpect(status().isCreated());

		mvc.perform(post("/api/auth/login").contentType("application/json")
			.content("{\"email\":\"" + email + "\",\"password\":\"pass1234\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(cookie().exists(CookieUtils.REFRESH_COOKIE));

		mvc.perform(post("/api/auth/login").contentType("application/json")
			.content("{\"email\":\"" + email + "\",\"password\":\"wrongpass1\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("LOGIN_FAILED"));

		users.findByEmail(email).ifPresent(u -> users.deleteById(u.getId()));
	}

}
