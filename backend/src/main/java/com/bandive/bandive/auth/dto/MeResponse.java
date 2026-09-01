package com.bandive.bandive.auth.dto;

import com.bandive.bandive.user.User;

public record MeResponse(Long id, String nickname) {

	public static MeResponse from(User user) {
		return new MeResponse(user.getId(), user.getNickname());
	}

}
