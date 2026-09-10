package com.bandive.bandive.band.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.band.MyRelation;
import com.bandive.bandive.band.dto.BandCreateRequest;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.dto.BandUpdateRequest;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.storage.StorageService;
import com.bandive.bandive.follow.BandFollowRepository;
import com.bandive.bandive.follow.FollowStatus;
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

	private final BandFollowRepository follows;

	public BandService(BandRepository bands, BandMemberRepository bandMembers, UserRepository users,
			StorageService storage, InviteCodeRepository inviteCodes, InviteCodeCache inviteCodeCache,
			BandFollowRepository follows) {
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.users = users;
		this.storage = storage;
		this.inviteCodes = inviteCodes;
		this.inviteCodeCache = inviteCodeCache;
		this.follows = follows;
	}

	/** 밴드 생성 — 만든 사람을 자동으로 OWNER 멤버로 등록. */
	@Transactional
	public BandResponse create(Long userId, BandCreateRequest request) {
		User owner = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Band band = bands.save(Band.builder()
			.name(request.name())
			.description(request.description())
			.visibility(request.visibility() != null ? request.visibility() : BandVisibility.PUBLIC)
			.build());
		bandMembers
			.save(BandMember.builder().band(band).user(owner).role(BandRole.OWNER).joinedAt(Instant.now()).build());

		return BandResponse.from(band, 1, BandRole.OWNER);
	}

	/**
	 * 밴드 표지 조회. PRIVATE 밴드는 멤버가 아니면 존재를 숨겨 404. FOLLOWERS/PUBLIC 은 표지(이름·소개)를 누구에게나 준다
	 * (콘텐츠는 각 스코프 GET 에서 별도 게이트).
	 */
	public BandResponse get(Long bandId, Long userId) {
		Band band = findBand(bandId);
		BandRole role = userId == null ? null
				: bandMembers.findByBandIdAndUserId(bandId, userId).map(BandMember::getRole).orElse(null);
		if (role == null && band.getVisibility() == BandVisibility.PRIVATE) {
			throw new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.");
		}
		return BandResponse.from(band, bandMembers.countByBandId(bandId), followerCount(bandId), role,
				myRelation(bandId, userId, role));
	}

	/** 승인된 팔로워 수. FOLLOWERS 밴드가 아니면 사실상 0. */
	private long followerCount(Long bandId) {
		return follows.countByBandIdAndStatus(bandId, FollowStatus.APPROVED);
	}

	private MyRelation myRelation(Long bandId, Long userId, BandRole role) {
		if (role != null) {
			return MyRelation.MEMBER;
		}
		if (userId == null) {
			return MyRelation.NONE;
		}
		return follows.findByBandIdAndUserId(bandId, userId)
			.map(f -> f.getStatus() == FollowStatus.APPROVED ? MyRelation.FOLLOWER : MyRelation.PENDING)
			.orElse(MyRelation.NONE);
	}

	@Transactional
	public BandResponse updateVisibility(Long bandId, BandVisibility visibility) {
		Band band = findBand(bandId);
		band.changeVisibility(visibility);
		return BandResponse.from(band, bandMembers.countByBandId(bandId), followerCount(bandId), BandRole.OWNER);
	}

	public List<BandResponse> myBands(Long userId) {
		return bandMembers.findAllByUserId(userId)
			.stream()
			.map(membership -> BandResponse.from(membership.getBand(),
					bandMembers.countByBandId(membership.getBand().getId()),
					followerCount(membership.getBand().getId()), membership.getRole()))
			.toList();
	}

	@Transactional
	public BandResponse update(Long bandId, BandUpdateRequest request) {
		Band band = findBand(bandId);
		band.updateInfo(request.name(), request.description());
		return BandResponse.from(band, bandMembers.countByBandId(bandId), followerCount(bandId), BandRole.OWNER);
	}

	@Transactional
	public BandResponse updateLogo(Long bandId, MultipartFile file) {
		Band band = findBand(bandId);
		String previous = band.getLogoUrl();
		band.changeLogo(storage.store(LOGO_DIR, file));
		storage.delete(previous);
		return BandResponse.from(band, bandMembers.countByBandId(bandId), followerCount(bandId), BandRole.OWNER);
	}

	@Transactional
	public BandResponse updateBanner(Long bandId, MultipartFile file) {
		Band band = findBand(bandId);
		String previous = band.getBannerUrl();
		band.changeBanner(storage.store(BANNER_DIR, file));
		storage.delete(previous);
		return BandResponse.from(band, bandMembers.countByBandId(bandId), followerCount(bandId), BandRole.OWNER);
	}

	/** 관리자 위임 — 대상은 OWNER, 이전 관리자는 MEMBER 로. 대상 == 본인이면 no-op. */
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
