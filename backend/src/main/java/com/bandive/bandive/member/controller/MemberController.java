package com.bandive.bandive.member.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.member.dto.MemberPartsRequest;
import com.bandive.bandive.member.dto.MemberResponse;
import com.bandive.bandive.member.service.MemberService;

@RestController
@RequestMapping("/api/bands/{bandId}/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping
	public List<MemberResponse> list(@PathVariable Long bandId) {
		return memberService.list(bandId);
	}

	/** 내 파트 설정/해제. literal "me" 라 아래 {userId} 패턴보다 우선 매칭된다. */
	@PatchMapping("/me")
	public MemberResponse updateMyParts(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody MemberPartsRequest request) {
		return memberService.updateMyParts(bandId, userId, request);
	}

	/** 밴드장이 특정 멤버의 파트 설정/해제. */
	@PatchMapping("/{userId}")
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public MemberResponse updateMemberParts(@PathVariable Long bandId, @PathVariable Long userId,
			@Valid @RequestBody MemberPartsRequest request) {
		return memberService.updateMemberParts(bandId, userId, request);
	}

	/** 탈퇴. */
	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable Long bandId, @CurrentUser Long userId) {
		memberService.leave(bandId, userId);
	}

	/** 추방 (밴드장). */
	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public void kick(@PathVariable Long bandId, @PathVariable Long userId) {
		memberService.kick(bandId, userId);
	}

}
