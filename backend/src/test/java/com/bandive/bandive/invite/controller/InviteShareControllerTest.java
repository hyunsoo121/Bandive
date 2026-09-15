package com.bandive.bandive.invite.controller;

import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.common.config.FrontendProperties;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.invite.dto.InvitePreviewResponse;
import com.bandive.bandive.invite.service.InviteService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카카오톡 등 메신저 크롤러 대상 OG 태그 HTML. 인증 없이 GET 이라 실제 SecurityConfig 의 인가 규칙은 이 테스트 범위 밖 —
 * JwtProvider 만 목으로 채워 필터체인 빈 그래프를 완성시키고, 테스트 전용 permitAll 체인으로 덮어쓴다
 * (InviteControllerTest 와 동일 패턴).
 */
@WebMvcTest(InviteShareController.class)
@EnableConfigurationProperties(FrontendProperties.class)
@TestPropertySource(properties = "app.frontend.base-url=https://bandive.o-r.kr")
class InviteShareControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private InviteService inviteService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@Test
	void 유효한_코드는_밴드명과_og_태그를_담은_HTML_을_준다() throws Exception {
		given(inviteService.preview("ABCD2345"))
			.willReturn(new InvitePreviewResponse("ABCD2345", 1L, "금요일의소음", "소개", "https://x/logo.png", 3));

		mvc.perform(get("/invite/ABCD2345"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("금요일의소음")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("og:image")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("https://bandive.o-r.kr/join/ABCD2345")));
	}

	@Test
	void 없는_코드여도_기본_문구로_join_으로_리다이렉트한다() throws Exception {
		willThrow(new NotFoundException("INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다.")).given(inviteService)
			.preview("ZZZZ9999");

		mvc.perform(get("/invite/ZZZZ9999"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("https://bandive.o-r.kr/join/ZZZZ9999")));
	}

	@Test
	void 밴드명에_HTML_이_들어있어도_이스케이프된다() throws Exception {
		given(inviteService.preview("XSS1234"))
			.willReturn(new InvitePreviewResponse("XSS1234", 1L, "<script>alert(1)</script>", null, null, 1));

		// 리다이렉트용 스크립트 태그 자체는 응답에 정상적으로 존재하므로, "<script> 자체가 없어야 한다"가 아니라
		// "밴드명이 들어간 payload 가 그대로(비escape) 삽입되지는 않아야 한다"를 확인한다.
		mvc.perform(get("/invite/XSS1234"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
			.andExpect(content()
				.string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))));
	}

	@TestConfiguration
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
			http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
			return http.build();
		}

	}

}
