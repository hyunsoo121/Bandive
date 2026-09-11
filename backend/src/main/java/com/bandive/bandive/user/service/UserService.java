package com.bandive.bandive.user.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;
import com.bandive.bandive.user.dto.UserProfileResponse;
import com.bandive.bandive.user.dto.UserProfileResponse.BandBrief;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository users;

	private final BandMemberRepository bandMembers;

	public UserService(UserRepository users, BandMemberRepository bandMembers) {
		this.users = users;
		this.bandMembers = bandMembers;
	}

	/** 다른 사람이 보는 공개 프로필 — 닉네임·사진·한줄소개 + 소속 밴드 목록. */
	public UserProfileResponse getProfile(Long userId) {
		User user = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
		List<BandBrief> bands = bandMembers.findAllByUserIdWithBand(userId)
			.stream()
			.sorted(Comparator.comparing(member -> member.getBand().getId()))
			.map(member -> new BandBrief(member.getBand().getId(), member.getBand().getName(),
					member.getBand().getLogoUrl(), bandMembers.countByBandId(member.getBand().getId()),
					member.getRole()))
			.toList();
		return new UserProfileResponse(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getBio(), bands);
	}

}
