package com.bandive.bandive.media.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaRepository;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.media.dto.MediaCreateRequest;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleRepository;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class MediaService {

	private final MediaRepository media;

	private final ScheduleRepository schedules;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final UserRepository users;

	public MediaService(MediaRepository media, ScheduleRepository schedules, BandRepository bands,
			BandMemberRepository bandMembers, UserRepository users) {
		this.media = media;
		this.schedules = schedules;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.users = users;
	}

	/** 공개범위 필터 적용. 밴드 멤버면 전부, 그 외(비회원·비멤버)는 LINK_PUBLIC 만. */
	public List<MediaResponse> list(Long bandId, Long scheduleId, Long currentUserId) {
		if (!bands.existsById(bandId)) {
			throw new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.");
		}
		boolean isMember = currentUserId != null && bandMembers.existsByBandIdAndUserId(bandId, currentUserId);
		return media.findVisible(bandId, scheduleId, isMember).stream().map(MediaResponse::from).toList();
	}

	@Transactional
	public MediaResponse create(Long bandId, Long userId, MediaCreateRequest request) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		requireMember(bandId, userId);
		User uploader = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Schedule schedule = resolveSchedule(request.scheduleId(), bandId);
		MediaVisibility visibility = request.visibility() != null ? request.visibility() : MediaVisibility.MEMBERS_ONLY;

		Media saved = media.save(Media.builder()
			.band(band)
			.schedule(schedule)
			.uploadedBy(uploader)
			.type(request.type())
			.externalUrl(request.externalUrl().trim())
			.platform(MediaPlatform.detect(request.externalUrl()))
			.visibility(visibility)
			.build());
		return MediaResponse.from(saved);
	}

	/** 공개 범위 변경 (밴드장). */
	@Transactional
	public MediaResponse changeVisibility(Long mediaId, Long userId, MediaVisibility visibility) {
		Media found = findMedia(mediaId);
		requireOwner(found.getBand().getId(), userId);
		found.changeVisibility(visibility);
		return MediaResponse.from(found);
	}

	/** 삭제 — 등록자 본인 또는 밴드장. */
	@Transactional
	public void delete(Long mediaId, Long userId) {
		Media found = findMedia(mediaId);
		if (!found.getUploadedBy().getId().equals(userId)) {
			requireOwner(found.getBand().getId(), userId);
		}
		media.delete(found);
	}

	private Schedule resolveSchedule(Long scheduleId, Long bandId) {
		if (scheduleId == null) {
			return null;
		}
		Schedule schedule = schedules.findById(scheduleId)
			.orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND", "연결할 일정을 찾을 수 없습니다."));
		if (!schedule.getBand().getId().equals(bandId)) {
			throw new ValidationException("SCHEDULE_BAND_MISMATCH", "다른 밴드의 일정에는 연결할 수 없습니다.");
		}
		return schedule;
	}

	private Media findMedia(Long mediaId) {
		return media.findById(mediaId).orElseThrow(() -> new NotFoundException("MEDIA_NOT_FOUND", "영상을 찾을 수 없습니다."));
	}

	private void requireMember(Long bandId, Long userId) {
		if (!bandMembers.existsByBandIdAndUserId(bandId, userId)) {
			throw new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다.");
		}
	}

	private void requireOwner(Long bandId, Long userId) {
		BandMember member = bandMembers.findByBandIdAndUserId(bandId, userId)
			.orElseThrow(() -> new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다."));
		if (member.getRole() != BandRole.OWNER) {
			throw new ForbiddenException("NOT_BAND_OWNER", "밴드장만 할 수 있습니다.");
		}
	}

}
