package com.bandive.bandive.member.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.security.BandGuard;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.member.dto.MemberResponse;
import com.bandive.bandive.member.service.MemberService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 멤버_목록은_공개다() throws Exception {
		given(memberService.list(1L)).willReturn(List.of(
				new MemberResponse(10L, "밴드장", BandRole.OWNER, List.of("GUITAR"),
						Instant.parse("2026-09-01T00:00:00Z")),
				new MemberResponse(11L, "멤버", BandRole.MEMBER, List.of(), Instant.parse("2026-09-02T00:00:00Z"))));

		mvc.perform(get("/api/bands/1/members"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].nickname").value("밴드장"))
			.andExpect(jsonPath("$[0].role").value("OWNER"))
			.andExpect(jsonPath("$[0].parts[0]").value("GUITAR"))
			.andExpect(jsonPath("$[1].userId").value(11));
	}

	@Test
	void 내_파트_설정은_200() throws Exception {
		given(memberService.updateMyParts(eq(1L), eq(7L), any())).willReturn(
				new MemberResponse(7L, "나", BandRole.MEMBER, List.of("BASS"), Instant.parse("2026-09-01T00:00:00Z")));

		mvc.perform(patch("/api/bands/1/members/me").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"parts\":[\"BASS\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.parts[0]").value("BASS"));
	}

	@Test
	void 남의_파트_설정은_밴드장이_아니면_403() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(false);

		mvc.perform(patch("/api/bands/1/members/9").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"parts\":[\"DRUM\"]}")).andExpect(status().isForbidden());
	}

	@Test
	void 추방은_밴드장이면_204() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(true);

		mvc.perform(delete("/api/bands/1/members/9").with(asUser(7L))).andExpect(status().isNoContent());
		then(memberService).should().kick(1L, 9L);
	}

	@Test
	void 추방은_밴드장이_아니면_403() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(false);

		mvc.perform(delete("/api/bands/1/members/9").with(asUser(7L)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void 없는_멤버_추방은_404() throws Exception {
		given(bandGuard.isOwner(1L)).willReturn(true);
		doThrow(new NotFoundException("MEMBER_NOT_FOUND", "해당 멤버를 찾을 수 없습니다.")).when(memberService).kick(1L, 9L);

		mvc.perform(delete("/api/bands/1/members/9").with(asUser(7L)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	void 탈퇴는_204_이고_내_id_로_호출된다() throws Exception {
		mvc.perform(delete("/api/bands/1/members/me").with(asUser(7L))).andExpect(status().isNoContent());
		then(memberService).should().leave(1L, 7L);
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
