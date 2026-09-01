package com.bandive.bandive.auth.dto;

/**
 * @param accessToken Bearer 로 쓸 access 토큰
 * @param expiresIn 만료까지 초
 */
public record AccessTokenResponse(String accessToken, long expiresIn) {
}
