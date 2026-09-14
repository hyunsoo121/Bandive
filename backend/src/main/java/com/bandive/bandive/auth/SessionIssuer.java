package com.bandive.bandive.auth;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.bandive.bandive.auth.dto.AccessTokenResponse;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.auth.jwt.RefreshToken;

/**
 * 로그인 성공 후 세션 개시 — refresh 토큰을 Redis + httpOnly 쿠키에 심는다. 카카오 성공 핸들러와 이메일 로그인/가입이 공유한다.
 */
@Component
public class SessionIssuer {

	private final JwtProvider jwtProvider;

	private final RefreshTokenStore refreshTokenStore;

	private final CookieUtils cookieUtils;

	public SessionIssuer(JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore, CookieUtils cookieUtils) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.cookieUtils = cookieUtils;
	}

	/** refresh 토큰만 발급 (access 는 프론트가 {@code /api/auth/refresh} 로 받아간다 — 카카오 흐름). */
	public void openRefreshSession(Long userId, HttpServletResponse response) {
		RefreshToken refresh = jwtProvider.createRefreshToken(userId);
		refreshTokenStore.save(userId, refresh.jti(), refresh.ttl());
		response.addHeader(HttpHeaders.SET_COOKIE,
				cookieUtils.refreshCookie(refresh.value(), refresh.ttl()).toString());
	}

	/** refresh 쿠키 + 즉시 쓸 access 토큰까지 (이메일 로그인/가입 흐름). */
	public AccessTokenResponse openSession(Long userId, HttpServletResponse response) {
		openRefreshSession(userId, response);
		return new AccessTokenResponse(jwtProvider.createAccessToken(userId), jwtProvider.accessTtl().toSeconds());
	}

}
