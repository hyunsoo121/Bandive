package com.bandive.bandive.user.dto;

import java.util.List;

import com.bandive.bandive.member.BandRole;

/**
 * 다른 사람이 보는 공개 프로필. 밴드는 이 앱에서 전부 링크로 열람 가능(초대제 가입) 하므로 소속 밴드 목록도 공개로 본다.
 */
public record UserProfileResponse(Long id, String nickname, String avatarUrl, String bio, List<BandBrief> bands) {

	public record BandBrief(Long id, String name, String logoUrl, long memberCount, BandRole role) {
	}

}
