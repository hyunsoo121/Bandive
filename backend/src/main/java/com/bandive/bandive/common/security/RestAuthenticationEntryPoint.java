package com.bandive.bandive.common.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bandive.bandive.common.response.ErrorResponse;

/**
 * 미인증 요청 — 로그인 페이지 리다이렉트 대신 {@link ErrorResponse} 스키마의 401 JSON. (필터 단계라
 * {@code GlobalExceptionHandler} 가 못 잡으므로 포맷을 여기서 맞춘다.)
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.",
				request.getRequestURI());
		objectMapper.writeValue(response.getWriter(), body);
	}

}
