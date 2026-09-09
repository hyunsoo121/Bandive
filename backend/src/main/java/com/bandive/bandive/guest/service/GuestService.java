package com.bandive.bandive.guest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.security.BandAccessGuard;
import com.bandive.bandive.guest.Guest;
import com.bandive.bandive.guest.GuestRepository;
import com.bandive.bandive.guest.dto.GuestResponse;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;

/**
 * 게스트 멤버 관리. 목록 조회는 공개, 등록·이름수정·삭제는 관리자만. 삭제 시 곡 세션 배정은 자동 해제(DB SET NULL), 일정 출결 행은
 * 삭제(DB CASCADE)된다.
 */
@Service
@Transactional(readOnly = true)
public class GuestService {

	private final GuestRepository guests;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final BandAccessGuard bandAccess;

	public GuestService(GuestRepository guests, BandRepository bands, BandMemberRepository bandMembers,
			BandAccessGuard bandAccess) {
		this.guests = guests;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.bandAccess = bandAccess;
	}

	public List<GuestResponse> list(Long bandId, Long viewerUserId) {
		bandAccess.requireCanViewContent(bandId, viewerUserId);
		return guests.findAllByBandIdOrderByNameAsc(bandId).stream().map(GuestResponse::from).toList();
	}

	@Transactional
	public GuestResponse create(Long bandId, Long userId, String name) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		requireOwner(bandId, userId);
		String trimmed = name.trim();
		requireNameFree(bandId, trimmed);
		Guest guest = guests.save(Guest.builder().band(band).name(trimmed).build());
		return GuestResponse.from(guest);
	}

	@Transactional
	public GuestResponse rename(Long bandId, Long guestId, Long userId, String name) {
		requireOwner(bandId, userId);
		Guest guest = find(bandId, guestId);
		String trimmed = name.trim();
		if (!trimmed.equals(guest.getName())) {
			requireNameFree(bandId, trimmed);
		}
		guest.rename(trimmed);
		return GuestResponse.from(guest);
	}

	private void requireNameFree(Long bandId, String name) {
		if (guests.existsByBandIdAndName(bandId, name)) {
			throw new ConflictException("GUEST_NAME_TAKEN", "이미 같은 이름의 게스트가 있습니다.");
		}
	}

	@Transactional
	public GuestResponse setSession(Long bandId, Long guestId, Long userId, String session) {
		requireOwner(bandId, userId);
		Guest guest = find(bandId, guestId);
		guest.changeSession(session == null || session.isBlank() ? null : session.trim());
		return GuestResponse.from(guest);
	}

	@Transactional
	public void delete(Long bandId, Long guestId, Long userId) {
		requireOwner(bandId, userId);
		guests.delete(find(bandId, guestId));
	}

	private Guest find(Long bandId, Long guestId) {
		return guests.findByIdAndBandId(guestId, bandId)
			.orElseThrow(() -> new NotFoundException("GUEST_NOT_FOUND", "게스트를 찾을 수 없습니다."));
	}

	private void requireOwner(Long bandId, Long userId) {
		BandMember member = bandMembers.findByBandIdAndUserId(bandId, userId)
			.orElseThrow(() -> new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다."));
		if (member.getRole() != BandRole.OWNER) {
			throw new ForbiddenException("NOT_BAND_OWNER", "관리자만 할 수 있습니다.");
		}
	}

}
