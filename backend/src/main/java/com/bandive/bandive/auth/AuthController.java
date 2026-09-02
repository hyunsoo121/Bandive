package com.bandive.bandive.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.dto.AccessTokenResponse;
import com.bandive.bandive.auth.dto.MeResponse;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.auth.jwt.RefreshToken;
import com.bandive.bandive.common.exception.BandiveException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

/**
 * 로그인 자체는 {@code /oauth2/authorization/kakao} → 카카오 → 성공 핸들러가 처리한다. 이 컨트롤러는 그 뒤의 토큰
 * 수명주기(재발급/로그아웃)와 내 정보 조회만 담당.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final JwtProvider jwtProvider;

	private final RefreshTokenStore refreshTokenStore;

	private final CookieUtils cookieUtils;

	private final UserRepository users;

	public AuthController(JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore, CookieUtils cookieUtils,
			UserRepository users) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.cookieUtils = cookieUtils;
		this.users = users;
	}

	/** refresh 쿠키로 새 access 를 발급하고 refresh 를 회전한다. */
	@PostMapping("/refresh")
	public AccessTokenResponse refresh(
			@CookieValue(name = CookieUtils.REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BandiveException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISSING", "다시 로그인해 주세요.");
		}

		Claims claims;
		try {
			claims = jwtProvider.parseRefresh(refreshToken);
		}
		catch (JwtException | IllegalArgumentException ex) {
			throw new BandiveException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "다시 로그인해 주세요.");
		}

		Long userId = Long.valueOf(claims.getSubject());
		if (!refreshTokenStore.matches(userId, claims.getId())) {
			throw new BandiveException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "세션이 만료되었습니다. 다시 로그인해 주세요.");
		}

		RefreshToken rotated = jwtProvider.createRefreshToken(userId);
		refreshTokenStore.save(userId, rotated.jti(), rotated.ttl());
		response.addHeader(HttpHeaders.SET_COOKIE,
				cookieUtils.refreshCookie(rotated.value(), rotated.ttl()).toString());

		String access = jwtProvider.createAccessToken(userId);
		return new AccessTokenResponse(access, jwtProvider.accessTtl().toSeconds());
	}

	/** refresh 세션을 Redis 에서 지우고 쿠키를 만료시킨다. */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@CookieValue(name = CookieUtils.REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			try {
				Long userId = Long.valueOf(jwtProvider.parseRefresh(refreshToken).getSubject());
				refreshTokenStore.delete(userId);
			}
			catch (JwtException | IllegalArgumentException ignored) {
				// 이미 못 쓰는 토큰이면 쿠키만 지우면 된다
			}
		}
		response.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.clearRefreshCookie().toString());
	}

	/** 현재 로그인 사용자. 미인증이면 SecurityConfig 규칙에 따라 401. */
	@GetMapping("/me")
	public MeResponse me(@CurrentUser Long userId) {
		User user = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
		return MeResponse.from(user);
	}

}
