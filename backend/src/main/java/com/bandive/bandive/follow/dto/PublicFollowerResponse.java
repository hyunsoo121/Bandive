package com.bandive.bandive.follow.dto;

import com.bandive.bandive.follow.BandFollow;

/** 누구나(콘텐츠 열람 가능한 사람) 볼 수 있는 팔로워 한 명 — 승인된 팔로워만, 관리자용 타임스탬프는 뺀 요약. */
public record PublicFollowerResponse(Long userId, String nickname, String avatarUrl) {

	public static PublicFollowerResponse from(BandFollow follow) {
		return new PublicFollowerResponse(follow.getUser().getId(), follow.getUser().getNickname(),
				follow.getUser().getAvatarUrl());
	}

}
