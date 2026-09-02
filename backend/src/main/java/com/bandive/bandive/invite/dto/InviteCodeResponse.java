package com.bandive.bandive.invite.dto;

import java.time.Instant;

import com.bandive.bandive.invite.InviteCode;

public record InviteCodeResponse(String code, String inviteUrl, Instant expiresAt, Integer maxUses, int usedCount) {

	public static InviteCodeResponse from(InviteCode inviteCode, String inviteUrl) {
		return new InviteCodeResponse(inviteCode.getCode(), inviteUrl, inviteCode.getExpiresAt(),
				inviteCode.getMaxUses(), inviteCode.getUsedCount());
	}

}
