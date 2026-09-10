package com.bandive.bandive.common.security;

import org.springframework.stereotype.Component;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.follow.BandFollowRepository;
import com.bandive.bandive.follow.FollowStatus;
import com.bandive.bandive.member.BandMemberRepository;

/**
 * 밴드 공개범위에 따른 콘텐츠 열람 게이트. 밴드 스코프 GET(곡·일정·멤버·게스트·폴더 등) 서비스 진입부에서 호출한다.
 * {@code @PreAuthorize} 용이 아니라 서비스가 직접 부른다 (익명 사용자도 통과할 수 있어야 하므로).
 */
@Component
public class BandAccessGuard {

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final BandFollowRepository follows;

	public BandAccessGuard(BandRepository bands, BandMemberRepository bandMembers, BandFollowRepository follows) {
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.follows = follows;
	}

	/** userId null = 익명. 밴드가 없으면 false 대신 아래 require* 에서 404 를 던지게 둔다. */
	public boolean canViewContent(Band band, Long userId) {
		if (band.getVisibility() == BandVisibility.PUBLIC) {
			return true;
		}
		if (userId == null) {
			return false;
		}
		if (bandMembers.existsByBandIdAndUserId(band.getId(), userId)) {
			return true;
		}
		return band.getVisibility() == BandVisibility.FOLLOWERS
				&& follows.existsByBandIdAndUserIdAndStatus(band.getId(), userId, FollowStatus.APPROVED);
	}

	public boolean canViewContent(Long bandId, Long userId) {
		return canViewContent(findBand(bandId), userId);
	}

	/**
	 * 콘텐츠를 못 보면 예외. PRIVATE 이면 존재 자체를 숨겨 {@code 404}, FOLLOWERS 면
	 * {@code 403 CONTENT_RESTRICTED}.
	 */
	public void requireCanViewContent(Long bandId, Long userId) {
		Band band = findBand(bandId);
		if (canViewContent(band, userId)) {
			return;
		}
		if (band.getVisibility() == BandVisibility.PRIVATE) {
			throw new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.");
		}
		throw new ForbiddenException("CONTENT_RESTRICTED", "이 밴드의 콘텐츠는 팔로워만 볼 수 있습니다.");
	}

	private Band findBand(Long bandId) {
		return bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
	}

}
