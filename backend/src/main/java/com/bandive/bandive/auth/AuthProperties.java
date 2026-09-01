package com.bandive.bandive.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.auth.*} 설정. JWT 수명, 로그인 후 프론트 복귀 URL, 쿠키/CORS.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Jwt jwt, Oauth2 oauth2, Cookie cookie, Cors cors) {

	public record Jwt(String secret, @DefaultValue("PT30M") Duration accessTtl,
			@DefaultValue("P14D") Duration refreshTtl) {
	}

	public record Oauth2(String successRedirect, String failureRedirect) {
	}

	public record Cookie(@DefaultValue("true") boolean secure) {
	}

	public record Cors(@DefaultValue("http://localhost:5173") String allowedOrigin) {
	}

}
