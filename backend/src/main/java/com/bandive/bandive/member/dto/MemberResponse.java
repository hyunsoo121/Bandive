package com.bandive.bandive.member.dto;

import java.time.Instant;
import java.util.List;

import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandRole;

public record MemberResponse(Long userId, String nickname, BandRole role, boolean leader, List<String> parts,
		Instant joinedAt) {

	public static MemberResponse from(BandMember member) {
		return new MemberResponse(member.getUser().getId(), member.getUser().getNickname(), member.getRole(),
				member.isLeader(), List.copyOf(member.getParts()), member.getJoinedAt());
	}

}
