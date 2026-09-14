package com.bandive.bandive.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import com.bandive.bandive.auth.AuthProperties;

/**
 * access / refresh JWT 발급·검증. HS256, 시크릿은 {@code app.auth.jwt.secret}.
 */
@Component
public class JwtProvider {

	private static final String CLAIM_TYPE = "type";

	private final SecretKey key;

	private final Duration accessTtl;

	private final Duration refreshTtl;

	public JwtProvider(AuthProperties properties) {
		String secret = properties.jwt().secret();
		if (secret == null || secret.startsWith("${") || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
					"app.auth.jwt.secret 이 설정되지 않았거나 32바이트 미만입니다. backend/.env 의 JWT_SECRET 을 확인하세요 "
							+ "(예: openssl rand -base64 48).");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(secretBytes);
		this.accessTtl = properties.jwt().accessTtl();
		this.refreshTtl = properties.jwt().refreshTtl();
	}

	public String createAccessToken(Long userId) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(CLAIM_TYPE, TokenType.ACCESS.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(accessTtl)))
			.signWith(key)
			.compact();
	}

	public RefreshToken createRefreshToken(Long userId) {
		Instant now = Instant.now();
		String jti = UUID.randomUUID().toString();
		String value = Jwts.builder()
			.subject(String.valueOf(userId))
			.id(jti)
			.claim(CLAIM_TYPE, TokenType.REFRESH.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(refreshTtl)))
			.signWith(key)
			.compact();
		return new RefreshToken(value, jti, refreshTtl);
	}

	public Claims parseAccess(String token) {
		return parse(token, TokenType.ACCESS);
	}

	public Claims parseRefresh(String token) {
		return parse(token, TokenType.REFRESH);
	}

	public Duration accessTtl() {
		return accessTtl;
	}

	private Claims parse(String token, TokenType expected) {
		Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		Claims claims = jws.getPayload();
		if (!expected.name().equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new io.jsonwebtoken.JwtException("예상한 토큰 종류가 아닙니다: " + expected);
		}
		return claims;
	}

}
