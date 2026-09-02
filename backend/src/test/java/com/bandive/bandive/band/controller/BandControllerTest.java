package com.bandive.bandive.band.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.service.BandService;
import com.bandive.bandive.common.security.BandGuard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BandController.class)
class BandControllerTest {

	private static final BandResponse SAMPLE = new BandResponse(1L, "내 밴드", "소개", null, null, 1,
			Instant.parse("2026-09-02T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private BandService bandService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	// JwtAuthenticationFilter(@Component, Filter)가 슬라이스에 딸려 올라와 JwtProvider 를 요구한다.
	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 밴드_생성은_201_과_생성_결과() throws Exception {
		given(bandService.create(eq(7L), any())).willReturn(SAMPLE);

		mvc.perform(post("/api/bands").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"내 밴드\",\"description\":\"소개\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.memberCount").value(1));
	}

	@Test
	void 이름_없이_생성하면_400_VALIDATION_ERROR() throws Exception {
		mvc.perform(post("/api/bands").with(asUser(7L)).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("밴드 이름은 필수입니다"));
	}

	@Test
	void 상세_조회는_비인증도_허용() throws Exception {
		given(bandService.get(1L)).willReturn(SAMPLE);

		mvc.perform(get("/api/bands/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("내 밴드"));
	}

	@Test
	void 내_밴드_목록() throws Exception {
		given(bandService.myBands(7L)).willReturn(List.of(SAMPLE));

		mvc.perform(get("/api/bands/my").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void 밴드장이_아니면_수정_403() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(false);

		mvc.perform(patch("/api/bands/1").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"새 이름\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void 밴드장이면_수정_성공() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(true);
		given(bandService.update(eq(1L), any())).willReturn(SAMPLE);

		mvc.perform(patch("/api/bands/1").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"새 이름\"}")).andExpect(status().isOk());
	}

	@Test
	void 로고_업로드는_밴드장만() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(true);
		given(bandService.updateLogo(eq(1L), any())).willReturn(SAMPLE);

		mvc.perform(multipart("/api/bands/1/logo")
			.file(new MockMultipartFile("file", "logo.png", "image/png", new byte[] { 1, 2, 3 }))
			.with(asUser(7L))).andExpect(status().isOk());
	}

	@TestConfiguration
	@EnableMethodSecurity
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
			http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
			return http.build();
		}

	}

}
