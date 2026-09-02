package com.bandive.bandive.schedule.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.common.security.BandGuard;
import com.bandive.bandive.schedule.ScheduleType;
import com.bandive.bandive.schedule.dto.ScheduleResponse;
import com.bandive.bandive.schedule.dto.ScheduleResponse.Counts;
import com.bandive.bandive.schedule.service.ScheduleService;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

	private static final ScheduleResponse SCHEDULE = new ScheduleResponse(3L, 1L, ScheduleType.REHEARSAL,
			Instant.parse("2026-10-01T10:00:00Z"), "연습실", 7L, "나", new Counts(1, 0, 0), null, List.of(),
			Instant.parse("2026-09-02T00:00:00Z"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ScheduleService scheduleService;

	@MockitoBean(name = "bandGuard")
	private BandGuard bandGuard;

	@MockitoBean
	private JwtProvider jwtProvider;

	private static RequestPostProcessor asUser(long id) {
		return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	void 일정_목록은_공개다() throws Exception {
		given(scheduleService.list(eq(1L), any())).willReturn(List.of(SCHEDULE));

		mvc.perform(get("/api/bands/1/schedules"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(3))
			.andExpect(jsonPath("$[0].counts.attending").value(1));
	}

	@Test
	void 일정_등록은_밴드_멤버면_201() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);
		given(scheduleService.create(eq(1L), eq(7L), any())).willReturn(SCHEDULE);

		mvc.perform(post("/api/bands/1/schedules").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"REHEARSAL\",\"dateTime\":\"2026-10-01T10:00:00Z\",\"location\":\"연습실\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(3));
	}

	@Test
	void 일정_등록은_비멤버면_403() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(false);

		mvc.perform(post("/api/bands/1/schedules").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"REHEARSAL\",\"dateTime\":\"2026-10-01T10:00:00Z\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void 일시_없이_등록하면_400() throws Exception {
		given(bandGuard.isMember(1L)).willReturn(true);

		mvc.perform(post("/api/bands/1/schedules").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"REHEARSAL\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 일정_수정() throws Exception {
		given(scheduleService.update(eq(3L), eq(7L), any())).willReturn(SCHEDULE);

		mvc.perform(patch("/api/schedules/3").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"location\":\"새 장소\"}")).andExpect(status().isOk());
	}

	@Test
	void 일정_삭제는_204() throws Exception {
		mvc.perform(delete("/api/schedules/3").with(asUser(7L))).andExpect(status().isNoContent());
		then(scheduleService).should().delete(3L, 7L);
	}

	@Test
	void 출결_등록() throws Exception {
		given(scheduleService.setAttendance(eq(3L), eq(7L), any())).willReturn(SCHEDULE);

		mvc.perform(post("/api/schedules/3/attendance").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ATTENDING\"}")).andExpect(status().isOk());
	}

	@Test
	void 잘못된_출결값은_400() throws Exception {
		mvc.perform(post("/api/schedules/3/attendance").with(asUser(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"MAYBE\"}")).andExpect(status().isBadRequest());
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
