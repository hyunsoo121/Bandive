package com.bandive.bandive.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;

/**
 * 메서드 보안용 밴드 소속/역할 검사. 사용 예:
 *
 * <pre>{@code
 * &#64;PreAuthorize("@bandGuard.isOwner(#bandId)")
 * }</pre>
 */
@Component("bandGuard")
public class BandGuard {

	private final BandMemberRepository bandMembers;

	public BandGuard(BandMemberRepository bandMembers) {
		this.bandMembers = bandMembers;
	}

	public boolean isMember(Long bandId) {
		Long userId = currentUserId();
		return userId != null && bandMembers.existsByBandIdAndUserId(bandId, userId);
	}

	public boolean isOwner(Long bandId) {
		Long userId = currentUserId();
		return userId != null && bandMembers.findByBandIdAndUserId(bandId, userId)
			.map(member -> member.getRole() == BandRole.OWNER)
			.orElse(false);
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
			return principal.getId();
		}
		return null;
	}

}
