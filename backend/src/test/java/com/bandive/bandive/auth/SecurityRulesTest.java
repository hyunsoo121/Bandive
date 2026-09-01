package com.bandive.bandive.auth;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.support.IntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityRulesTest extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 헬스체크는_공개() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void GET_api_는_비회원도_통과한다() throws Exception {
		// 컨트롤러가 아직 없어 404 지만, 401/403 이 아니라는 게 핵심 (보안 통과)
		mvc.perform(get("/api/bands/1")).andExpect(status().isNotFound());
	}

	@Test
	void 쓰기_요청은_토큰_없으면_401() throws Exception {
		mvc.perform(post("/api/bands")).andExpect(status().isUnauthorized());
	}

	@Test
	void 쓰기_요청은_잘못된_토큰이면_401() throws Exception {
		mvc.perform(post("/api/bands").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 쓰기_요청은_유효한_access_토큰이면_보안을_통과한다() throws Exception {
		String token = jwtProvider.createAccessToken(1L);
		mvc.perform(post("/api/bands").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isNotFound());
	}

	@Test
	void auth_me_는_토큰_없으면_401() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

}
