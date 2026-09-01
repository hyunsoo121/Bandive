package com.bandive.bandive.auth.jwt;

import java.time.Duration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.AuthProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

	private static final String SECRET = "jwt-secret-for-tests-0123456789-0123456789-abcdef";

	private final JwtProvider provider = new JwtProvider(props(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

	@Test
	void access_토큰은_subject_와_타입을_담는다() {
		String token = provider.createAccessToken(42L);

		Claims claims = provider.parseAccess(token);
		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
	}

	@Test
	void refresh_토큰은_jti_를_가지며_회전마다_달라진다() {
		RefreshToken first = provider.createRefreshToken(1L);
		RefreshToken second = provider.createRefreshToken(1L);

		assertThat(first.jti()).isNotBlank().isNotEqualTo(second.jti());
		assertThat(provider.parseRefresh(first.value()).getId()).isEqualTo(first.jti());
	}

	@Test
	void access_토큰을_refresh_로_파싱하면_거부된다() {
		String access = provider.createAccessToken(1L);
		assertThatThrownBy(() -> provider.parseRefresh(access)).isInstanceOf(JwtException.class);
	}

	@Test
	void 다른_시크릿으로_서명된_토큰은_검증에_실패한다() {
		String foreign = new JwtProvider(
				props("another-secret-0123456789-0123456789-abcdefgh", Duration.ofMinutes(30), Duration.ofDays(14)))
			.createAccessToken(1L);

		assertThatThrownBy(() -> provider.parseAccess(foreign)).isInstanceOf(JwtException.class);
	}

	@Test
	void 만료된_토큰은_거부된다() {
		JwtProvider shortLived = new JwtProvider(props(SECRET, Duration.ofMillis(1), Duration.ofDays(14)));
		String token = shortLived.createAccessToken(1L);

		await();
		assertThatThrownBy(() -> shortLived.parseAccess(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void 시크릿이_너무_짧으면_생성에_실패한다() {
		assertThatThrownBy(() -> new JwtProvider(props("short", Duration.ofMinutes(1), Duration.ofDays(1))))
			.isInstanceOf(IllegalStateException.class);
	}

	private static AuthProperties props(String secret, Duration access, Duration refresh) {
		return new AuthProperties(new AuthProperties.Jwt(secret, access, refresh),
				new AuthProperties.Oauth2("http://localhost:5173/oauth/success", "http://localhost:5173/oauth/failure"),
				new AuthProperties.Cookie(false), new AuthProperties.Cors("http://localhost:5173"));
	}

	private static void await() {
		try {
			Thread.sleep(20);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
