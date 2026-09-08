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
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.media.dto.MediaCreateRequest;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.media.dto.MediaUpdateRequest;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleRepository;
import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongRepository;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class MediaService {

	private final MediaRepository media;

	private final ScheduleRepository schedules;

	private final SongRepository songs;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final UserRepository users;

	public MediaService(MediaRepository media, ScheduleRepository schedules, SongRepository songs, BandRepository bands,
			BandMemberRepository bandMembers, UserRepository users) {
		this.media = media;
		this.schedules = schedules;
		this.songs = songs;
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
		Song song = resolveSong(request.songId(), bandId);
		MediaVisibility visibility = request.visibility() != null ? request.visibility() : MediaVisibility.MEMBERS_ONLY;

		Media saved = media.save(Media.builder()
			.band(band)
			.schedule(schedule)
			.song(song)
			.uploadedBy(uploader)
			.type(request.type())
			.externalUrl(request.externalUrl().trim())
			.title(trimToNull(request.title()))
			.platform(MediaPlatform.detect(request.externalUrl()))
			.visibility(visibility)
			.build());
		return MediaResponse.from(saved);
	}

	/**
	 * 부분 수정 — 등록자 본인 또는 관리자. null 필드는 유지. URL 이 바뀌면 platform 을 다시 판별한다.
	 */
	@Transactional
	public MediaResponse update(Long mediaId, Long userId, MediaUpdateRequest request) {
		Media found = findMedia(mediaId);
		requireUploaderOrOwner(found, userId);

		String url = request.externalUrl() != null ? request.externalUrl().trim() : found.getExternalUrl();
		MediaPlatform platform = request.externalUrl() != null ? MediaPlatform.detect(url) : found.getPlatform();
		MediaType type = request.type() != null ? request.type() : found.getType();
		MediaVisibility visibility = request.visibility() != null ? request.visibility() : found.getVisibility();
		String title = request.title() != null ? trimToNull(request.title()) : found.getTitle();
		Schedule schedule = request.scheduleId() != null
				? resolveSchedule(request.scheduleId(), found.getBand().getId()) : found.getSchedule();
		Song song = request.songId() != null ? resolveSong(request.songId(), found.getBand().getId()) : found.getSong();

		found.edit(url, platform, type, visibility, title, schedule, song);
		return MediaResponse.from(found);
	}

	/** 공개 범위 변경 — 등록자 본인 또는 관리자. */
	@Transactional
	public MediaResponse changeVisibility(Long mediaId, Long userId, MediaVisibility visibility) {
		Media found = findMedia(mediaId);
		requireUploaderOrOwner(found, userId);
		found.changeVisibility(visibility);
		return MediaResponse.from(found);
	}

	/** 삭제 — 등록자 본인 또는 관리자. */
	@Transactional
	public void delete(Long mediaId, Long userId) {
		Media found = findMedia(mediaId);
		requireUploaderOrOwner(found, userId);
		media.delete(found);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void requireUploaderOrOwner(Media media, Long userId) {
		if (!media.getUploadedBy().getId().equals(userId)) {
			requireOwner(media.getBand().getId(), userId);
		}
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

	private Song resolveSong(Long songId, Long bandId) {
		if (songId == null) {
			return null;
		}
		Song song = songs.findById(songId)
			.orElseThrow(() -> new NotFoundException("SONG_NOT_FOUND", "연결할 곡을 찾을 수 없습니다."));
		if (!song.getBand().getId().equals(bandId)) {
			throw new ValidationException("SONG_BAND_MISMATCH", "다른 밴드의 곡에는 연결할 수 없습니다.");
		}
		if (song.getStatus() != SongStatus.CONFIRMED) {
			throw new ValidationException("SONG_NOT_CONFIRMED", "합주곡(확정된 곡)만 영상에 연결할 수 있습니다.");
		}
		return song;
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
			throw new ForbiddenException("NOT_BAND_OWNER", "관리자만 할 수 있습니다.");
		}
	}

}
