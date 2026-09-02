package com.bandive.bandive.invite.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.common.security.BandGuard;
import com.bandive.bandive.invite.dto.InviteCodeResponse;
import com.bandive.bandive.invite.service.InviteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InviteController.class)
class InviteControllerTest {

	private static final InviteCodeResponse CODE = new InviteCodeResponse("ABCD2345",
			"http://localhost:5173/invite/ABCD2345", null, null, 0);

	private static final BandResponse BAND = new BandResponse(1L, "내 밴드", null, null, null, 2,
			Instant.parse("2026-09-02T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private InviteService inviteService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 밴드장은_초대코드를_발급받는다() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(true);
		given(inviteService.issue(1L)).willReturn(CODE);

		mvc.perform(post("/api/bands/1/invite-codes").with(asUser(7L)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("ABCD2345"))
			.andExpect(jsonPath("$.inviteUrl").value("http://localhost:5173/invite/ABCD2345"));
	}

	@Test
	void 밴드장이_아니면_발급_403() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(false);

		mvc.perform(post("/api/bands/1/invite-codes").with(asUser(7L)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void 코드로_가입하면_밴드정보를_돌려준다() throws Exception {
		given(inviteService.join(eq(7L), eq("ABCD2345"))).willReturn(BAND);

		mvc.perform(post("/api/invite-codes/ABCD2345/join").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.memberCount").value(2));
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
