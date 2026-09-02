package com.bandive.bandive.song.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.common.security.BandGuard;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.dto.SongResponse;
import com.bandive.bandive.song.dto.TrackSearchResult;
import com.bandive.bandive.song.dto.VoteResult;
import com.bandive.bandive.song.service.SongService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SongController.class)
class SongControllerTest {

	private static final SongResponse SONG = new SongResponse(5L, 1L, "곡", "아티스트", SongStatus.WISHLIST,
			SongSourceType.MANUAL, null, "메모", null, 7L, "나", 3, true, List.of(),
			Instant.parse("2026-09-02T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private SongService songService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 음원_검색은_공개다() throws Exception {
		given(songService.search("yes")).willReturn(List.of(new TrackSearchResult("stub:yes:1", "yes (샘플)", "아티스트")));

		mvc.perform(get("/api/songs/search").param("q", "yes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].externalTrackId").value("stub:yes:1"));
	}

	@Test
	void 곡_목록은_공개다() throws Exception {
		given(songService.list(eq(1L), any(), any())).willReturn(List.of(SONG));

		mvc.perform(get("/api/bands/1/songs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(5))
			.andExpect(jsonPath("$[0].voteCount").value(3));
	}

	@Test
	void 곡_추가는_밴드_멤버면_201() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);
		given(songService.add(eq(1L), eq(7L), any())).willReturn(SONG);

		mvc.perform(post("/api/bands/1/songs").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"곡\",\"sourceType\":\"MANUAL\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void 곡_추가는_비멤버면_403() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(false);

		mvc.perform(post("/api/bands/1/songs").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"곡\",\"sourceType\":\"MANUAL\"}")).andExpect(status().isForbidden());
	}

	@Test
	void 제목_없이_추가하면_400() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);

		mvc.perform(post("/api/bands/1/songs").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"sourceType\":\"MANUAL\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 투표는_카운트를_돌려준다() throws Exception {
		given(songService.vote(5L, 7L)).willReturn(new VoteResult(4, true));

		mvc.perform(post("/api/songs/5/vote").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.voteCount").value(4))
			.andExpect(jsonPath("$.votedByMe").value(true));
	}

	@Test
	void 투표_취소() throws Exception {
		given(songService.unvote(5L, 7L)).willReturn(new VoteResult(3, false));

		mvc.perform(delete("/api/songs/5/vote").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.votedByMe").value(false));
	}

	@Test
	void 승격() throws Exception {
		given(songService.confirm(5L, 7L)).willReturn(SONG);

		mvc.perform(patch("/api/songs/5/confirm").with(asUser(7L))).andExpect(status().isOk());
	}

	@Test
	void 파트_배정() throws Exception {
		given(songService.assignPart(5L, 9L, 7L, 12L)).willReturn(SONG);

		mvc.perform(put("/api/songs/5/parts/9/assign").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"userId\":12}")).andExpect(status().isOk());
	}

	@Test
	void 곡_삭제는_204() throws Exception {
		mvc.perform(delete("/api/songs/5").with(asUser(7L))).andExpect(status().isNoContent());
		then(songService).should().delete(5L, 7L);
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
