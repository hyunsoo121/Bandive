package com.bandive.bandive.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰 쿠키 생성/삭제. httpOnly + SameSite=Strict + path=/api/auth 로 노출을 최소화한다.
 */
@Component
public class CookieUtils {

	public static final String REFRESH_COOKIE = "refresh_token";

	private static final String PATH = "/api/auth";

	private final boolean secure;

	public CookieUtils(AuthProperties properties) {
		this.secure = properties.cookie().secure();
	}

	public ResponseCookie refreshCookie(String value, Duration ttl) {
		return build(value, ttl);
	}

	public ResponseCookie clearRefreshCookie() {
		return build("", Duration.ZERO);
	}

	private ResponseCookie build(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Strict")
			.path(PATH)
			.maxAge(maxAge)
			.build();
	}

}
