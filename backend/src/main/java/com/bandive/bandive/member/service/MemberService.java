package com.bandive.bandive.member.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.member.dto.MemberPartsRequest;
import com.bandive.bandive.member.dto.MemberResponse;

@Service
@Transactional(readOnly = true)
public class MemberService {

	/** OWNER 먼저 → 가입 순. */
	private static final Comparator<BandMember> DISPLAY_ORDER = Comparator
		.comparing((BandMember member) -> member.getRole() == BandRole.OWNER ? 0 : 1)
		.thenComparing(BandMember::getJoinedAt);

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	public MemberService(BandRepository bands, BandMemberRepository bandMembers) {
		this.bands = bands;
		this.bandMembers = bandMembers;
	}

	public List<MemberResponse> list(Long bandId) {
		if (!bands.existsById(bandId)) {
			throw new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.");
		}
		return bandMembers.findAllByBandIdWithUser(bandId)
			.stream()
			.sorted(DISPLAY_ORDER)
			.map(MemberResponse::from)
			.toList();
	}

	/** 내 파트 설정/해제. */
	@Transactional
	public MemberResponse updateMyParts(Long bandId, Long userId, MemberPartsRequest request) {
		BandMember me = requireMember(bandId, userId, "NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다.");
		me.replaceParts(request.parts());
		return MemberResponse.from(me);
	}

	/** 밴드장이 특정 멤버의 파트 설정/해제. */
	@Transactional
	public MemberResponse updateMemberParts(Long bandId, Long targetUserId, MemberPartsRequest request) {
		BandMember target = requireMember(bandId, targetUserId, "MEMBER_NOT_FOUND", "해당 멤버를 찾을 수 없습니다.");
		target.replaceParts(request.parts());
		return MemberResponse.from(target);
	}

	/** 밴드장이 다른 멤버를 추방. */
	@Transactional
	public void kick(Long bandId, Long targetUserId) {
		BandMember target = requireMember(bandId, targetUserId, "MEMBER_NOT_FOUND", "해당 멤버를 찾을 수 없습니다.");
		if (target.getRole() == BandRole.OWNER) {
			throw new ConflictException("CANNOT_KICK_OWNER", "밴드장은 추방할 수 없습니다.");
		}
		bandMembers.delete(target);
	}

	/** 본인이 밴드에서 탈퇴. */
	@Transactional
	public void leave(Long bandId, Long userId) {
		BandMember me = requireMember(bandId, userId, "NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다.");
		if (me.getRole() == BandRole.OWNER) {
			throw new ConflictException("OWNER_CANNOT_LEAVE", "밴드장은 탈퇴할 수 없습니다. 밴드를 삭제하거나 밴드장을 위임하세요.");
		}
		bandMembers.delete(me);
	}

	private BandMember requireMember(Long bandId, Long userId, String code, String message) {
		return bandMembers.findByBandIdAndUserId(bandId, userId)
			.orElseThrow(() -> new NotFoundException(code, message));
	}

}
