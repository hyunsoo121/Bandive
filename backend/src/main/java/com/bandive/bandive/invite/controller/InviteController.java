package com.bandive.bandive.invite.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.invite.dto.InviteCodeResponse;
import com.bandive.bandive.invite.service.InviteService;

@RestController
public class InviteController {

	private final InviteService inviteService;

	public InviteController(InviteService inviteService) {
		this.inviteService = inviteService;
	}

	/** 초대 코드 발급/재발급 (밴드장). */
	@PostMapping("/api/bands/{bandId}/invite-codes")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public InviteCodeResponse issue(@PathVariable Long bandId) {
		return inviteService.issue(bandId);
	}

	/** 초대 코드로 가입 (로그인). */
	@PostMapping("/api/invite-codes/{code}/join")
	public BandResponse join(@CurrentUser Long userId, @PathVariable String code) {
		return inviteService.join(userId, code);
	}

}
