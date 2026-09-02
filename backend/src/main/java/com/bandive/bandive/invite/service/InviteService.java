package com.bandive.bandive.invite.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.common.config.FrontendProperties;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.invite.InviteCode;
import com.bandive.bandive.invite.InviteCodeRepository;
import com.bandive.bandive.invite.dto.InviteCodeResponse;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class InviteService {

	private final InviteCodeRepository inviteCodes;

	private final InviteCodeGenerator generator;

	private final InviteCodeCache cache;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final UserRepository users;

	private final String frontendBaseUrl;

	public InviteService(InviteCodeRepository inviteCodes, InviteCodeGenerator generator, InviteCodeCache cache,
			BandRepository bands, BandMemberRepository bandMembers, UserRepository users,
			FrontendProperties frontendProperties) {
		this.inviteCodes = inviteCodes;
		this.generator = generator;
		this.cache = cache;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.users = users;
		this.frontendBaseUrl = trimTrailingSlash(frontendProperties.baseUrl());
	}

	/** 밴드당 코드 1개. 이미 있으면 폐기하고 새로 발급. */
	@Transactional
	public InviteCodeResponse issue(Long bandId) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));

		inviteCodes.findByBandId(bandId).ifPresent(existing -> {
			cache.evict(existing.getCode());
			inviteCodes.delete(existing);
		});

		String code = generator.generateUnique();
		InviteCode saved = inviteCodes.save(InviteCode.builder().band(band).code(code).build());
		cache.put(code, bandId);

		return InviteCodeResponse.from(saved, inviteUrl(code));
	}

	/** 코드로 가입 — MEMBER 로 등록. */
	@Transactional
	public BandResponse join(Long userId, String code) {
		User user = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Long bandId = resolveBandId(code);
		if (bandMembers.existsByBandIdAndUserId(bandId, userId)) {
			throw new ConflictException("ALREADY_MEMBER", "이미 이 밴드의 멤버입니다.");
		}

		InviteCode inviteCode = inviteCodes.findByCode(code)
			.orElseThrow(() -> new NotFoundException("INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다."));
		Band band = inviteCode.getBand();

		bandMembers
			.save(BandMember.builder().band(band).user(user).role(BandRole.MEMBER).joinedAt(Instant.now()).build());
		inviteCode.incrementUsedCount();

		return BandResponse.from(band, bandMembers.countByBandId(bandId));
	}

	private Long resolveBandId(String code) {
		return cache.findBandId(code).orElseGet(() -> {
			InviteCode inviteCode = inviteCodes.findByCode(code)
				.orElseThrow(() -> new NotFoundException("INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다."));
			Long bandId = inviteCode.getBand().getId();
			cache.put(code, bandId);
			return bandId;
		});
	}

	private String inviteUrl(String code) {
		return frontendBaseUrl + "/invite/" + code;
	}

	private static String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

}
