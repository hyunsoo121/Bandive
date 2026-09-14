package com.bandive.bandive.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.support.IntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GlobalExceptionHandlerTest extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtProvider jwtProvider;

	private String bearer() {
		return "Bearer " + jwtProvider.createAccessToken(1L);
	}

	@Test
	void NotFoundException_은_404_와_도메인_코드() throws Exception {
		mvc.perform(get("/api/__test__/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("THING_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("그건 없어요."))
			.andExpect(jsonPath("$.path").value("/api/__test__/not-found"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void ConflictException_은_409() throws Exception {
		mvc.perform(get("/api/__test__/conflict"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_VOTED"));
	}

	@Test
	void 예상못한_예외는_500_이고_내부_메시지를_숨긴다() throws Exception {
		mvc.perform(get("/api/__test__/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.message").value(not(containsString("내부 상세"))));
	}

	@Test
	void 잘못된_HTTP_메서드는_405() throws Exception {
		mvc.perform(get("/api/__test__/only-post").header(HttpHeaders.AUTHORIZATION, bearer()))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void Bean_Validation_실패는_400_과_필드_메시지() throws Exception {
		mvc.perform(post("/api/__test__/validate").header(HttpHeaders.AUTHORIZATION, bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("이름은 필수입니다"));
	}

	@Test
	void 깨진_JSON_본문은_400() throws Exception {
		mvc.perform(post("/api/__test__/validate").header(HttpHeaders.AUTHORIZATION, bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("not-json")).andExpect(status().isBadRequest());
	}

	@TestConfiguration
	static class TestControllerConfig {

		@Bean
		TestController testController() {
			return new TestController();
		}

	}

	@RestController
	static class TestController {

		@GetMapping("/api/__test__/not-found")
		void notFound() {
			throw new NotFoundException("THING_NOT_FOUND", "그건 없어요.");
		}

		@GetMapping("/api/__test__/conflict")
		void conflict() {
			throw new ConflictException("ALREADY_VOTED", "이미 투표했어요.");
		}

		@GetMapping("/api/__test__/boom")
		void boom() {
			throw new IllegalStateException("내부 상세 - 노출되면 안 됨");
		}

		@PostMapping("/api/__test__/only-post")
		void onlyPost() {
		}

		@PostMapping("/api/__test__/validate")
		void validate(@Valid @RequestBody TestBody body) {
		}

	}

	record TestBody(@NotBlank(message = "이름은 필수입니다") String name) {
	}

}
