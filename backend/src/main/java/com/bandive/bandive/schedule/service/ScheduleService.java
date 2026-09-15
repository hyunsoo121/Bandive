package com.bandive.bandive.schedule.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.security.BandAccessGuard;
import com.bandive.bandive.guest.Guest;
import com.bandive.bandive.guest.GuestRepository;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaRepository;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.notification.NotificationType;
import com.bandive.bandive.notification.service.NotificationService;
import com.bandive.bandive.schedule.Attendance;
import com.bandive.bandive.schedule.AttendanceRepository;
import com.bandive.bandive.schedule.AttendanceStatus;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleRepository;
import com.bandive.bandive.schedule.dto.ScheduleCreateRequest;
import com.bandive.bandive.schedule.dto.ScheduleResponse;
import com.bandive.bandive.schedule.dto.ScheduleUpdateRequest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

	private final ScheduleRepository schedules;

	private final AttendanceRepository attendances;

	private final MediaRepository media;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final GuestRepository guests;

	private final UserRepository users;

	private final BandAccessGuard bandAccess;

	private final NotificationService notificationService;

	public ScheduleService(ScheduleRepository schedules, AttendanceRepository attendances, MediaRepository media,
			BandRepository bands, BandMemberRepository bandMembers, GuestRepository guests, UserRepository users,
			BandAccessGuard bandAccess, NotificationService notificationService) {
		this.schedules = schedules;
		this.attendances = attendances;
		this.media = media;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.guests = guests;
		this.users = users;
		this.bandAccess = bandAccess;
		this.notificationService = notificationService;
	}

	public List<ScheduleResponse> list(Long bandId, Long currentUserId) {
		bandAccess.requireCanViewContent(bandId, currentUserId);
		List<Schedule> found = schedules.findAllForBand(bandId);
		if (found.isEmpty()) {
			return List.of();
		}
		List<Long> ids = found.stream().map(Schedule::getId).toList();
		Map<Long, List<Attendance>> attendeesBySchedule = attendances.findAllByScheduleIds(ids)
			.stream()
			.collect(Collectors.groupingBy(attendance -> attendance.getSchedule().getId()));

		boolean isMember = currentUserId != null && bandMembers.existsByBandIdAndUserId(bandId, currentUserId);
		Map<Long, List<Media>> mediaBySchedule = media.findVisibleByScheduleIds(ids, isMember)
			.stream()
			.collect(Collectors.groupingBy(item -> item.getSchedule().getId()));

		return found.stream()
			.map(schedule -> ScheduleResponse.from(schedule,
					attendeesBySchedule.getOrDefault(schedule.getId(), List.of()),
					mediaBySchedule.getOrDefault(schedule.getId(), List.of()), currentUserId))
			.toList();
	}

	@Transactional
	public ScheduleResponse create(Long bandId, Long userId, ScheduleCreateRequest request) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		requireMember(bandId, userId);
		User creator = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Schedule schedule = schedules.save(Schedule.builder()
			.band(band)
			.createdBy(creator)
			.type(request.type())
			.dateTime(request.dateTime())
			.location(trimToNull(request.location()))
			.build());
		notificationService.notifyBandMembers(band, NotificationType.SCHEDULE_CREATED, creator, userId);
		return ScheduleResponse.from(schedule, List.of(), List.of(), userId);
	}

	@Transactional
	public ScheduleResponse update(Long scheduleId, Long userId, ScheduleUpdateRequest request) {
		Schedule schedule = findSchedule(scheduleId);
		requireMember(schedule.getBand().getId(), userId);
		schedule.updateInfo(request.type(), request.dateTime(), request.location());
		return toResponse(schedule, userId);
	}

	@Transactional
	public void delete(Long scheduleId, Long userId) {
		Schedule schedule = findSchedule(scheduleId);
		requireOwner(schedule.getBand().getId(), userId);
		schedules.delete(schedule);
	}

	/** 내 참석 여부 등록/변경 (upsert). */
	@Transactional
	public ScheduleResponse setAttendance(Long scheduleId, Long userId, AttendanceStatus status) {
		Schedule schedule = findSchedule(scheduleId);
		requireMember(schedule.getBand().getId(), userId);
		upsertAttendance(schedule, userId, status);
		return toResponse(schedule, userId);
	}

	/** 관리자가 특정 멤버의 참석 여부를 대신 등록/변경 (upsert). */
	@Transactional
	public ScheduleResponse setMemberAttendance(Long scheduleId, Long actorUserId, Long targetUserId,
			AttendanceStatus status) {
		Schedule schedule = findSchedule(scheduleId);
		Long bandId = schedule.getBand().getId();
		requireOwner(bandId, actorUserId);
		if (!bandMembers.existsByBandIdAndUserId(bandId, targetUserId)) {
			throw new NotFoundException("MEMBER_NOT_FOUND", "해당 멤버를 찾을 수 없습니다.");
		}
		upsertAttendance(schedule, targetUserId, status);
		return toResponse(schedule, actorUserId);
	}

	private void upsertAttendance(Schedule schedule, Long userId, AttendanceStatus status) {
		attendances.findByScheduleIdAndUserId(schedule.getId(), userId)
			.ifPresentOrElse(existing -> existing.changeStatus(status),
					() -> attendances.save(Attendance.builder()
						.schedule(schedule)
						.user(users.getReferenceById(userId))
						.status(status)
						.build()));
	}

	/** 관리자가 게스트를 일정에 추가 (이미 있으면 그대로). 게스트는 항상 참석(ATTENDING). */
	@Transactional
	public ScheduleResponse setGuestAttendance(Long scheduleId, Long actorUserId, Long guestId) {
		Schedule schedule = findSchedule(scheduleId);
		Long bandId = schedule.getBand().getId();
		requireOwner(bandId, actorUserId);
		Guest guest = guests.findByIdAndBandId(guestId, bandId)
			.orElseThrow(() -> new NotFoundException("GUEST_NOT_FOUND", "게스트를 찾을 수 없습니다."));
		if (attendances.findByScheduleIdAndGuestId(scheduleId, guestId).isEmpty()) {
			attendances
				.save(Attendance.builder().schedule(schedule).guest(guest).status(AttendanceStatus.ATTENDING).build());
		}
		return toResponse(schedule, actorUserId);
	}

	/** 관리자가 게스트를 일정에서 제외 (참석 행 삭제). */
	@Transactional
	public ScheduleResponse removeGuestAttendance(Long scheduleId, Long actorUserId, Long guestId) {
		Schedule schedule = findSchedule(scheduleId);
		requireOwner(schedule.getBand().getId(), actorUserId);
		attendances.findByScheduleIdAndGuestId(scheduleId, guestId).ifPresent(attendances::delete);
		return toResponse(schedule, actorUserId);
	}

	private Schedule findSchedule(Long scheduleId) {
		return schedules.findById(scheduleId)
			.orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND", "일정을 찾을 수 없습니다."));
	}

	private ScheduleResponse toResponse(Schedule schedule, Long currentUserId) {
		// update/setAttendance 를 부른 시점엔 currentUserId 가 멤버로 검증돼 있음 → 멤버전용 영상까지 포함
		List<Media> linkedMedia = media.findVisible(schedule.getBand().getId(), schedule.getId(), true);
		return ScheduleResponse.from(schedule, attendances.findAllByScheduleId(schedule.getId()), linkedMedia,
				currentUserId);
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

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

}
