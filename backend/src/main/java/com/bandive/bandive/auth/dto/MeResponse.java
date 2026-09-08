package com.bandive.bandive.auth.dto;

import com.bandive.bandive.user.AuthProvider;
import com.bandive.bandive.user.User;

/**
 * 현재 로그인 사용자. {@code email} 은 LOCAL 가입자만 값이 있고 카카오 가입자는 null. {@code provider} 로 프론트가
 * 비밀번호 변경 UI 노출 여부를 정한다.
 */
public record MeResponse(Long id, String nickname, String email, AuthProvider provider) {

	public static MeResponse from(User user) {
		return new MeResponse(user.getId(), user.getNickname(), user.getEmail(), user.getProvider());
	}

}
