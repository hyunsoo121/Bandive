package com.bandive.bandive.common.config;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.support.IntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CorsConfigTest extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void 허용_오리진의_preflight_는_credentials_와_함께_통과() throws Exception {
		mvc.perform(options("/api/bands").header("Origin", "http://localhost:5173")
			.header("Access-Control-Request-Method", "POST"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
			.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@Test
	void 허용되지_않은_오리진의_preflight_는_거부() throws Exception {
		mvc.perform(options("/api/bands").header("Origin", "https://evil.example.com")
			.header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
	}

}
