package com.bandive.bandive.auth;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰 저장소 (Redis). 유저당 1세션 — {@code refresh:{userId}} 에 현재 유효한 jti 하나만 둔다. 새
 * 로그인/회전 시 덮어써서 이전 refresh 를 무효화한다.
 */
@Component
public class RefreshTokenStore {

	private final StringRedisTemplate redis;

	public RefreshTokenStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void save(Long userId, String jti, Duration ttl) {
		redis.opsForValue().set(key(userId), jti, ttl);
	}

	public boolean matches(Long userId, String jti) {
		String stored = redis.opsForValue().get(key(userId));
		return stored != null && stored.equals(jti);
	}

	public void delete(Long userId) {
		redis.delete(key(userId));
	}

	private String key(Long userId) {
		return "refresh:" + userId;
	}

}
