package com.bandive.bandive.follow.dto;

import java.time.Instant;

import com.bandive.bandive.follow.BandFollow;
import com.bandive.bandive.follow.FollowStatus;

/** 관리자에게 보여줄 팔로워/요청자 한 건. */
public record FollowerResponse(Long userId, String nickname, FollowStatus status, Instant requestedAt,
		Instant decidedAt) {

	public static FollowerResponse from(BandFollow follow) {
		return new FollowerResponse(follow.getUser().getId(), follow.getUser().getNickname(), follow.getStatus(),
				follow.getCreatedAt(), follow.getDecidedAt());
	}

}
