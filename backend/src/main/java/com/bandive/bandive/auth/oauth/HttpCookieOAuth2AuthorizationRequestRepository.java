package com.bandive.bandive.auth.oauth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 authorization request 를 세션 대신 단명 쿠키에 담는다. API 를 STATELESS 로 유지하기 위함. 쿠키는 우리 서버만
 * 읽고(HttpOnly) 3분 안에 콜백이 돌아오므로 수명을 짧게 둔다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String COOKIE_NAME = "oauth2_auth_request";

	private static final int COOKIE_MAX_AGE_SECONDS = 180;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return readCookie(request).map(HttpCookieOAuth2AuthorizationRequestRepository::deserialize).orElse(null);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
			HttpServletResponse response) {
		if (authorizationRequest == null) {
			removeAuthorizationRequestCookies(request, response);
			return;
		}
		addCookie(response, COOKIE_NAME, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS);
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
			HttpServletResponse response) {
		OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
		if (loaded != null) {
			removeAuthorizationRequestCookies(request, response);
		}
		return loaded;
	}

	public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
		addCookie(response, COOKIE_NAME, "", 0);
	}

	private Optional<String> readCookie(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(StringUtils::hasText)
			.findFirst();
	}

	private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}

	private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(authorizationRequest);
			out.flush();
			return Base64.getUrlEncoder().encodeToString(bytes.toByteArray());
		}
		catch (IOException ex) {
			throw new IllegalStateException("authorization request 직렬화 실패", ex);
		}
	}

	private static OAuth2AuthorizationRequest deserialize(String encoded) {
		byte[] data = Base64.getUrlDecoder().decode(encoded);
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data)) {
			@Override
			protected Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
				String name = desc.getName();
				if (!name.startsWith("org.springframework.security.") && !name.startsWith("java.")
						&& !name.startsWith("[") && !name.startsWith("org.springframework.util.")) {
					throw new java.io.InvalidClassException("허용되지 않은 클래스 역직렬화: " + name);
				}
				return super.resolveClass(desc);
			}
		}) {
			return (OAuth2AuthorizationRequest) in.readObject();
		}
		catch (IOException | ClassNotFoundException ex) {
			return null;
		}
	}

}
