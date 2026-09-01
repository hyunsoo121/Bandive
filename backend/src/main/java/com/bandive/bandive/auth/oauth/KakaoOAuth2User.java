package com.bandive.bandive.auth.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 카카오 로그인 성공 직후의 principal. 우리 DB 의 userId 를 함께 들고 있어 성공 핸들러가 JWT 를 발급할 수 있다.
 */
public final class KakaoOAuth2User implements OAuth2User {

	private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

	private final Long userId;

	private final Map<String, Object> attributes;

	public KakaoOAuth2User(Long userId, Map<String, Object> attributes) {
		this.userId = userId;
		this.attributes = attributes;
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AUTHORITIES;
	}

	@Override
	public String getName() {
		return String.valueOf(userId);
	}

}
