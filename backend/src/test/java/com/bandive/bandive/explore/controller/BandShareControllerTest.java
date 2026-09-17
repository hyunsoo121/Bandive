package com.bandive.bandive.explore.controller;

import java.util.List;

import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.band.MyRelation;
import com.bandive.bandive.common.config.FrontendProperties;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.explore.dto.ExploreBandDetailResponse;
import com.bandive.bandive.explore.dto.ExploreBandResponse;
import com.bandive.bandive.explore.service.ExploreService;

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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 카카오톡 등 메신저 크롤러 대상 OG 태그 HTML. InviteShareControllerTest 와 동일 패턴. */
@WebMvcTest(BandShareController.class)
@EnableConfigurationProperties(FrontendProperties.class)
@TestPropertySource(properties = "app.frontend.base-url=https://bandive.o-r.kr")
class BandShareControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ExploreService exploreService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@Test
	void 공개_밴드는_이름과_og_태그를_담은_HTML_을_준다() throws Exception {
		ExploreBandResponse card = new ExploreBandResponse(5L, "금요일의소음", "소개", "https://x/logo.png",
				BandVisibility.PUBLIC, 3);
		given(exploreService.bandDetail(5L, null))
			.willReturn(new ExploreBandDetailResponse(card, MyRelation.NONE, List.of()));

		mvc.perform(get("/band/5"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("금요일의소음")))
			.andExpect(content().string(containsString("og:image")))
			.andExpect(content().string(containsString("https://bandive.o-r.kr/explore/bands/5")));
	}

	@Test
	void 비공개거나_없는_밴드도_기본_문구로_구경화면으로_리다이렉트한다() throws Exception {
		willThrow(new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.")).given(exploreService).bandDetail(9L, null);

		mvc.perform(get("/band/9"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("https://bandive.o-r.kr/explore/bands/9")));
	}

	@Test
	void 밴드명에_HTML_이_들어있어도_이스케이프된다() throws Exception {
		ExploreBandResponse card = new ExploreBandResponse(7L, "<script>alert(1)</script>", null, null,
				BandVisibility.PUBLIC, 1);
		given(exploreService.bandDetail(7L, null))
			.willReturn(new ExploreBandDetailResponse(card, MyRelation.NONE, List.of()));

		mvc.perform(get("/band/7"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script>alert(1)</script>"))));
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
