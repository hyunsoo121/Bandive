package com.bandive.bandive.auth;

import java.util.Locale;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bandive.bandive.auth.dto.AccessTokenResponse;
import com.bandive.bandive.auth.dto.LoginRequest;
import com.bandive.bandive.auth.dto.MeResponse;
import com.bandive.bandive.auth.dto.PasswordChangeRequest;
import com.bandive.bandive.auth.dto.ProfileUpdateRequest;
import com.bandive.bandive.auth.dto.SignupRequest;
import com.bandive.bandive.auth.jwt.JwtProvider;
import com.bandive.bandive.auth.jwt.RefreshToken;
import com.bandive.bandive.common.exception.BandiveException;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.common.storage.StorageService;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

/**
 * 로그인은 카카오({@code /oauth2/authorization/kakao} → 성공 핸들러) 또는 이메일({@code /signup},
 * {@code /login}). 나머지는 토큰 수명주기(재발급/로그아웃)와 내 정보 조회.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final JwtProvider jwtProvider;

	private final RefreshTokenStore refreshTokenStore;

	private final CookieUtils cookieUtils;

	private final SessionIssuer sessionIssuer;

	private final PasswordEncoder passwordEncoder;

	private final UserRepository users;

	private final StorageService storage;

	public AuthController(JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore, CookieUtils cookieUtils,
			SessionIssuer sessionIssuer, PasswordEncoder passwordEncoder, UserRepository users,
			StorageService storage) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.cookieUtils = cookieUtils;
		this.sessionIssuer = sessionIssuer;
		this.passwordEncoder = passwordEncoder;
		this.users = users;
		this.storage = storage;
	}

	/** 이메일 회원가입 → 바로 로그인 상태로 (refresh 쿠키 + access). */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AccessTokenResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			throw new ConflictException("EMAIL_TAKEN", "이미 가입된 이메일입니다.");
		}
		User user = users
			.save(User.ofLocal(email, passwordEncoder.encode(request.password()), request.nickname().trim()));
		return sessionIssuer.openSession(user.getId(), response);
	}

	/** 이메일 로그인. 이메일/비밀번호 어느 쪽이 틀렸는지는 구분하지 않는다. */
	@PostMapping("/login")
	public AccessTokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		User user = users.findByEmail(email)
			.filter(u -> u.getPasswordHash() != null)
			.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
			.orElseThrow(
					() -> new BandiveException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다."));
		return sessionIssuer.openSession(user.getId(), response);
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
		return MeResponse.from(findUser(userId));
	}

	/** 내 정보 수정 — 닉네임 + 한 줄 소개. */
	@PatchMapping("/me")
	@Transactional
	public MeResponse updateMe(@CurrentUser Long userId, @Valid @RequestBody ProfileUpdateRequest request) {
		User user = findUser(userId);
		user.updateNickname(request.nickname().trim());
		user.updateBio(trimToNull(request.bio()));
		return MeResponse.from(user);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** 프로필 사진 업로드/교체. 이전 파일은 정리한다. */
	@PostMapping("/me/avatar")
	@Transactional
	public MeResponse uploadAvatar(@CurrentUser Long userId, @RequestParam("file") MultipartFile file) {
		User user = findUser(userId);
		String previous = user.getAvatarUrl();
		user.updateAvatar(storage.store("avatars", file));
		storage.delete(previous);
		return MeResponse.from(user);
	}

	/** 프로필 사진 제거 → 이니셜 아바타로. */
	@DeleteMapping("/me/avatar")
	@Transactional
	public MeResponse removeAvatar(@CurrentUser Long userId) {
		User user = findUser(userId);
		String previous = user.getAvatarUrl();
		user.updateAvatar(null);
		storage.delete(previous);
		return MeResponse.from(user);
	}

	/** 비밀번호 변경 (이메일 로그인 계정만). 현재 비밀번호가 맞아야 한다. */
	@PatchMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void changePassword(@CurrentUser Long userId, @Valid @RequestBody PasswordChangeRequest request) {
		User user = findUser(userId);
		if (!user.isLocal()) {
			throw new ConflictException("PASSWORD_CHANGE_UNSUPPORTED", "이메일 로그인 계정만 비밀번호를 바꿀 수 있습니다.");
		}
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ValidationException("CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 올바르지 않습니다.");
		}
		user.changePassword(passwordEncoder.encode(request.newPassword()));
	}

	private User findUser(Long userId) {
		return users.findById(userId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
	}

}
