package com.bandive.bandive.media.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.common.security.BandGuard;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.media.dto.MediaLikeResult;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.media.service.MediaService;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
class MediaControllerTest {

	private static final String JSON = "application/json";

	private static final MediaResponse MEDIA = new MediaResponse(5L, 1L, null, null, null, MediaType.PERFORMANCE,
			"https://youtu.be/x", "공연 영상", MediaPlatform.YOUTUBE, "https://img.youtube.com/vi/x/hqdefault.jpg",
			MediaVisibility.MEMBERS_ONLY, 7L, "나", 0L, false, false, Instant.parse("2026-09-02T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private MediaService mediaService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 영상_목록은_공개다() throws Exception {
		given(mediaService.list(eq(1L), any(), any())).willReturn(List.of(MEDIA));

		mvc.perform(get("/api/bands/1/media"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(5))
			.andExpect(jsonPath("$[0].platform").value("YOUTUBE"));
	}

	@Test
	void 등록은_밴드_멤버면_201() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);
		given(mediaService.create(eq(1L), eq(7L), any())).willReturn(MEDIA);

		mvc.perform(post("/api/bands/1/media").with(asUser(7L))
			.contentType(JSON)
			.content("{\"externalUrl\":\"https://youtu.be/x\",\"type\":\"PERFORMANCE\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void 등록은_비멤버면_403() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(false);

		mvc.perform(post("/api/bands/1/media").with(asUser(7L))
			.contentType(JSON)
			.content("{\"externalUrl\":\"https://youtu.be/x\",\"type\":\"PERFORMANCE\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void URL_형식이_아니면_400() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);

		mvc.perform(post("/api/bands/1/media").with(asUser(7L))
			.contentType(JSON)
			.content("{\"externalUrl\":\"not a url\",\"type\":\"PERFORMANCE\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 공개범위_변경() throws Exception {
		given(mediaService.changeVisibility(eq(5L), eq(7L), eq(MediaVisibility.LINK_PUBLIC))).willReturn(MEDIA);

		mvc.perform(patch("/api/media/5/visibility").with(asUser(7L))
			.contentType(JSON)
			.content("{\"visibility\":\"LINK_PUBLIC\"}")).andExpect(status().isOk());
	}

	@Test
	void 부분수정_PATCH() throws Exception {
		given(mediaService.update(eq(5L), eq(7L), any())).willReturn(MEDIA);

		mvc.perform(patch("/api/media/5").with(asUser(7L))
			.contentType(JSON)
			.content("{\"title\":\"새 제목\",\"type\":\"REHEARSAL\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("공연 영상"));
	}

	@Test
	void 부분수정_URL_형식_틀리면_400() throws Exception {
		mvc.perform(patch("/api/media/5").with(asUser(7L)).contentType(JSON).content("{\"externalUrl\":\"ftp://x\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 삭제는_204() throws Exception {
		mvc.perform(delete("/api/media/5").with(asUser(7L))).andExpect(status().isNoContent());
		then(mediaService).should().delete(5L, 7L);
	}

	@Test
	void 좋아요는_로그인하면_카운트를_돌려준다() throws Exception {
		given(mediaService.like(5L, 7L)).willReturn(new MediaLikeResult(3L, true));

		mvc.perform(post("/api/media/5/like").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(3))
			.andExpect(jsonPath("$.likedByMe").value(true));
	}

	@Test
	void 좋아요_취소() throws Exception {
		given(mediaService.unlike(5L, 7L)).willReturn(new MediaLikeResult(2L, false));

		mvc.perform(delete("/api/media/5/like").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(2))
			.andExpect(jsonPath("$.likedByMe").value(false));
	}

	@Test
	void 고정() throws Exception {
		given(mediaService.pin(5L, 7L)).willReturn(MEDIA);

		mvc.perform(post("/api/media/5/pin").with(asUser(7L))).andExpect(status().isOk());
	}

	@Test
	void 고정_해제() throws Exception {
		given(mediaService.unpin(5L, 7L)).willReturn(MEDIA);

		mvc.perform(delete("/api/media/5/pin").with(asUser(7L))).andExpect(status().isOk());
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
