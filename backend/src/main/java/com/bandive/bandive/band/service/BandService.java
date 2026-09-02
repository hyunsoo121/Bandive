package com.bandive.bandive.band.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.dto.BandCreateRequest;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.dto.BandUpdateRequest;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.storage.StorageService;
import com.bandive.bandive.invite.InviteCodeRepository;
import com.bandive.bandive.invite.service.InviteCodeCache;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class BandService {

	private static final String LOGO_DIR = "band-logo";

	private static final String BANNER_DIR = "band-banner";

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final UserRepository users;

	private final StorageService storage;

	private final InviteCodeRepository inviteCodes;

	private final InviteCodeCache inviteCodeCache;

	public BandService(BandRepository bands, BandMemberRepository bandMembers, UserRepository users,
			StorageService storage, InviteCodeRepository inviteCodes, InviteCodeCache inviteCodeCache) {
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.users = users;
		this.storage = storage;
		this.inviteCodes = inviteCodes;
		this.inviteCodeCache = inviteCodeCache;
	}

	/** 밴드 생성 — 만든 사람을 자동으로 OWNER 멤버로 등록. */
	@Transactional
	public BandResponse create(Long userId, BandCreateRequest request) {
		User owner = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Band band = bands.save(Band.builder().name(request.name()).description(request.description()).build());
		bandMembers
			.save(BandMember.builder().band(band).user(owner).role(BandRole.OWNER).joinedAt(Instant.now()).build());

		return BandResponse.from(band, 1);
	}

	public BandResponse get(Long bandId) {
		Band band = findBand(bandId);
		return BandResponse.from(band, bandMembers.countByBandId(bandId));
	}

	public List<BandResponse> myBands(Long userId) {
		return bandMembers.findAllByUserId(userId)
			.stream()
			.map(BandMember::getBand)
			.map(band -> BandResponse.from(band, bandMembers.countByBandId(band.getId())))
			.toList();
	}

	@Transactional
	public BandResponse update(Long bandId, BandUpdateRequest request) {
		Band band = findBand(bandId);
		band.updateInfo(request.name(), request.description());
		return BandResponse.from(band, bandMembers.countByBandId(bandId));
	}

	@Transactional
	public BandResponse updateLogo(Long bandId, MultipartFile file) {
		Band band = findBand(bandId);
		String previous = band.getLogoUrl();
		band.changeLogo(storage.store(LOGO_DIR, file));
		storage.delete(previous);
		return BandResponse.from(band, bandMembers.countByBandId(bandId));
	}

	@Transactional
	public BandResponse updateBanner(Long bandId, MultipartFile file) {
		Band band = findBand(bandId);
		String previous = band.getBannerUrl();
		band.changeBanner(storage.store(BANNER_DIR, file));
		storage.delete(previous);
		return BandResponse.from(band, bandMembers.countByBandId(bandId));
	}

	/** 밴드장 위임 — 대상은 OWNER, 이전 밴드장은 MEMBER 로. 대상 == 본인이면 no-op. */
	@Transactional
	public void transferOwnership(Long bandId, Long currentOwnerId, Long targetUserId) {
		if (currentOwnerId.equals(targetUserId)) {
			return;
		}
		BandMember target = bandMembers.findByBandIdAndUserId(bandId, targetUserId)
			.orElseThrow(() -> new NotFoundException("MEMBER_NOT_FOUND", "위임할 멤버를 찾을 수 없습니다."));
		BandMember current = bandMembers.findByBandIdAndUserId(bandId, currentOwnerId)
			.orElseThrow(() -> new NotFoundException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다."));
		target.changeRole(BandRole.OWNER);
		current.changeRole(BandRole.MEMBER);
	}

	/** 밴드 삭제 — 하위(멤버/초대/곡/일정/미디어)는 DB FK CASCADE 로 함께 삭제. 파일·Redis 초대키는 직접 정리. */
	@Transactional
	public void delete(Long bandId) {
		Band band = findBand(bandId);

		inviteCodes.findByBandId(bandId).ifPresent(inviteCode -> {
			inviteCodeCache.evict(inviteCode.getCode());
			inviteCodes.delete(inviteCode);
		});
		storage.delete(band.getLogoUrl());
		storage.delete(band.getBannerUrl());

		bands.delete(band);
	}

	private Band findBand(Long bandId) {
		return bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
	}

}
