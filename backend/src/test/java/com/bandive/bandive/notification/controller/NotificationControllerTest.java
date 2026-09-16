package com.bandive.bandive.notification.controller;

import java.time.Instant;
import java.util.List;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.notification.NotificationType;
import com.bandive.bandive.notification.dto.NotificationResponse;
import com.bandive.bandive.notification.service.NotificationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 없이 GET/POST 시도 시 401 인지는 실제 SecurityConfig 몫이라 이 슬라이스 범위 밖 — 여기서는 컨트롤러가 서비스에 올바르게
 * 위임하는지만 확인한다.
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

	private static final NotificationResponse SAMPLE = new NotificationResponse(1L, NotificationType.FOLLOW_REQUESTED,
			2L, "내 밴드", 7L, "홍길동", null, Instant.parse("2026-09-16T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 목록을_준다() throws Exception {
		given(notificationService.list(7L)).willReturn(List.of(SAMPLE));

		mvc.perform(get("/api/notifications").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].type").value("FOLLOW_REQUESTED"))
			.andExpect(jsonPath("$[0].bandName").value("내 밴드"))
			.andExpect(jsonPath("$[0].actorNickname").value("홍길동"));
	}

	@Test
	void 안읽은_개수를_준다() throws Exception {
		given(notificationService.unreadCount(7L)).willReturn(3L);

		mvc.perform(get("/api/notifications/unread-count").with(asUser(7L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(3));
	}

	@Test
	void 하나_읽음_처리한다() throws Exception {
		mvc.perform(post("/api/notifications/1/read").with(asUser(7L))).andExpect(status().isNoContent());

		then(notificationService).should().markRead(1L, 7L);
	}

	@Test
	void 전체_읽음_처리한다() throws Exception {
		mvc.perform(post("/api/notifications/read-all").with(asUser(7L))).andExpect(status().isNoContent());

		then(notificationService).should().markAllRead(7L);
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
