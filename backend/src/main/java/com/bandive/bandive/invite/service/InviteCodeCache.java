package com.bandive.bandive.invite.service;

import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code invite:{code}} → {@code bandId} 리졸브 캐시. 코드 하나로 밴드를 빠르게 찾기 위한 것. 만료 정책이 없으므로 TTL
 * 없이 두고, 재발급 시 명시적으로 evict 한다. 미스는 DB 에서 다시 채운다.
 */
@Component
public class InviteCodeCache {

	private final StringRedisTemplate redis;

	public InviteCodeCache(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void put(String code, Long bandId) {
		redis.opsForValue().set(key(code), String.valueOf(bandId));
	}

	public Optional<Long> findBandId(String code) {
		String value = redis.opsForValue().get(key(code));
		return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
	}

	public void evict(String code) {
		redis.delete(key(code));
	}

	private String key(String code) {
		return "invite:" + code;
	}

}
