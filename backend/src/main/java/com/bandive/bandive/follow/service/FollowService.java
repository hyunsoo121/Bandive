package com.bandive.bandive.follow.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.follow.BandFollow;
import com.bandive.bandive.follow.BandFollowRepository;
import com.bandive.bandive.follow.FollowStatus;
import com.bandive.bandive.follow.dto.FollowerResponse;
import com.bandive.bandive.follow.dto.FollowingBandResponse;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.user.UserRepository;

/**
 * 승인제 팔로우. 요청/취소는 로그인 유저 본인이, 목록·승인·거절은 관리자가. 알림 시스템은 없다 (관리자가 멤버 페이지에서 요청 목록을 확인·처리).
 */
@Service
@Transactional(readOnly = true)
public class FollowService {

	private final BandFollowRepository follows;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final UserRepository users;

	public FollowService(BandFollowRepository follows, BandRepository bands, BandMemberRepository bandMembers,
			UserRepository users) {
		this.follows = follows;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.users = users;
	}

	/** 팔로우 요청 (PENDING). FOLLOWERS 밴드만. 이미 요청/승인돼 있으면 그대로. */
	@Transactional
	public void request(Long bandId, Long userId) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		if (band.getVisibility() != BandVisibility.FOLLOWERS) {
			throw new ConflictException("FOLLOW_NOT_AVAILABLE", "이 밴드는 팔로우를 받지 않습니다.");
		}
		if (bandMembers.existsByBandIdAndUserId(bandId, userId)) {
			throw new ConflictException("ALREADY_MEMBER", "이미 이 밴드의 멤버입니다.");
		}
		if (follows.findByBandIdAndUserId(bandId, userId).isPresent()) {
			return;
		}
		follows.save(BandFollow.builder()
			.band(band)
			.user(users.getReferenceById(userId))
			.status(FollowStatus.PENDING)
			.build());
	}

	/** 요청 취소 / 언팔로우 (행 삭제). 없으면 no-op. */
	@Transactional
	public void cancel(Long bandId, Long userId) {
		follows.deleteByBandIdAndUserId(bandId, userId);
	}

	/** 관리자 — 팔로워/요청 목록. status null 이면 전체. */
	public List<FollowerResponse> listFollowers(Long bandId, Long ownerId, FollowStatus status) {
		requireOwner(bandId, ownerId);
		return follows.findAllForBand(bandId, status).stream().map(FollowerResponse::from).toList();
	}

	/** 로그인 유저 본인이 팔로우한 밴드 목록 (요청 대기 + 승인 모두). */
	public List<FollowingBandResponse> listMyFollowing(Long userId) {
		return follows.findFollowingByUser(userId).stream().map(FollowingBandResponse::of).toList();
	}

	/** 관리자 — 팔로우 요청 승인. */
	@Transactional
	public void approve(Long bandId, Long ownerId, Long targetUserId) {
		requireOwner(bandId, ownerId);
		BandFollow follow = follows.findByBandIdAndUserId(bandId, targetUserId)
			.orElseThrow(() -> new NotFoundException("FOLLOW_REQUEST_NOT_FOUND", "팔로우 요청을 찾을 수 없습니다."));
		follow.approve();
	}

	/** 관리자 — 요청 거절 또는 팔로워 제거 (행 삭제). */
	@Transactional
	public void remove(Long bandId, Long ownerId, Long targetUserId) {
		requireOwner(bandId, ownerId);
		follows.deleteByBandIdAndUserId(bandId, targetUserId);
	}

	private void requireOwner(Long bandId, Long userId) {
		BandMember member = userId == null ? null : bandMembers.findByBandIdAndUserId(bandId, userId).orElse(null);
		if (member == null) {
			throw new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다.");
		}
		if (member.getRole() != BandRole.OWNER) {
			throw new ForbiddenException("NOT_BAND_OWNER", "관리자만 할 수 있습니다.");
		}
	}

}
