package com.bandive.bandive.auth.jwt;

import java.time.Duration;

/**
 * 발급된 refresh 토큰 값 + Redis 저장에 쓸 jti + 만료 기간.
 */
public record RefreshToken(String value, String jti, Duration ttl) {
}
