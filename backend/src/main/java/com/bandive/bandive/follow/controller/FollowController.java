package com.bandive.bandive.follow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.follow.FollowStatus;
import com.bandive.bandive.follow.dto.FollowerResponse;
import com.bandive.bandive.follow.dto.FollowingBandResponse;
import com.bandive.bandive.follow.dto.PublicFollowerResponse;
import com.bandive.bandive.follow.service.FollowService;

@RestController
public class FollowController {

	private final FollowService followService;

	public FollowController(FollowService followService) {
		this.followService = followService;
	}

	/** 팔로우 요청 (PRIVATE 밴드만 불가). FOLLOWERS 는 승인 대기, PUBLIC 은 즉시 승인. */
	@PostMapping("/api/bands/{bandId}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void follow(@PathVariable Long bandId, @CurrentUser Long userId) {
		followService.request(bandId, userId);
	}

	/** 요청 취소 / 언팔로우. */
	@DeleteMapping("/api/bands/{bandId}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollow(@PathVariable Long bandId, @CurrentUser Long userId) {
		followService.cancel(bandId, userId);
	}

	/** 관리자 — 팔로워/요청 목록. {@code status=PENDING} 이면 대기 중만. */
	@GetMapping("/api/bands/{bandId}/followers")
	public List<FollowerResponse> followers(@PathVariable Long bandId,
			@RequestParam(name = "status", required = false) FollowStatus status,
			@AuthenticationPrincipal UserPrincipal principal) {
		return followService.listFollowers(bandId, principal != null ? principal.getId() : null, status);
	}

	/** 공개 팔로워 목록 (승인된 팔로워만) — 이 밴드 콘텐츠를 볼 수 있는 사람이면 누구나(비로그인 포함). */
	@GetMapping("/api/bands/{bandId}/followers/public")
	public List<PublicFollowerResponse> publicFollowers(@PathVariable Long bandId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return followService.listPublicFollowers(bandId, principal != null ? principal.getId() : null);
	}

	/** 내가 팔로우한 밴드 목록 (요청 대기 + 승인). */
	@GetMapping("/api/me/following")
	public List<FollowingBandResponse> myFollowing(@CurrentUser Long userId) {
		return followService.listMyFollowing(userId);
	}

	/** 관리자 — 팔로우 요청 승인. */
	@PutMapping("/api/bands/{bandId}/followers/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void approve(@PathVariable Long bandId, @PathVariable Long userId, @CurrentUser Long ownerId) {
		followService.approve(bandId, ownerId, userId);
	}

	/** 관리자 — 요청 거절 또는 팔로워 제거. */
	@DeleteMapping("/api/bands/{bandId}/followers/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long bandId, @PathVariable Long userId, @CurrentUser Long ownerId) {
		followService.remove(bandId, ownerId, userId);
	}

}
