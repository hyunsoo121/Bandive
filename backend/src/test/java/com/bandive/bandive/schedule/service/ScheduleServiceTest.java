package com.bandive.bandive.schedule.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.schedule.AttendanceRepository;
import com.bandive.bandive.schedule.AttendanceStatus;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleRepository;
import com.bandive.bandive.schedule.ScheduleType;
import com.bandive.bandive.schedule.dto.ScheduleCreateRequest;
import com.bandive.bandive.schedule.dto.ScheduleResponse;
import com.bandive.bandive.schedule.dto.ScheduleUpdateRequest;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleServiceTest extends RepositoryTest {

	@Autowired
	private ScheduleRepository schedules;

	@Autowired
	private AttendanceRepository attendances;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private com.bandive.bandive.guest.GuestRepository guests;

	@Autowired
	private com.bandive.bandive.user.UserRepository users;

	@Autowired
	private com.bandive.bandive.media.MediaRepository media;

	@Autowired
	private TestEntityManager em;

	private ScheduleService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new ScheduleService(schedules, attendances, media, bands, bandMembers, guests, users);
		band = em.persist(Fixtures.band("A"));
		ownerId = joinMember("owner", BandRole.OWNER);
		memberId = joinMember("member", BandRole.MEMBER);
	}

	private Long joinMember(String kakaoId, BandRole role) {
		User user = em.persist(Fixtures.user(kakaoId));
		em.persist(Fixtures.member(band, user, role));
		return user.getId();
	}

	private ScheduleCreateRequest req() {
		return new ScheduleCreateRequest(ScheduleType.REHEARSAL, Instant.parse("2026-10-01T10:00:00Z"), "연습실");
	}

	@Test
	void 등록하면_생성자가_기록된다() {
		ScheduleResponse created = service.create(band.getId(), memberId, req());

		assertThat(created.createdByUserId()).isEqualTo(memberId);
		assertThat(created.type()).isEqualTo(ScheduleType.REHEARSAL);
		assertThat(created.location()).isEqualTo("연습실");
		assertThat(created.attendees()).isEmpty();
	}

	@Test
	void 비멤버는_일정을_등록할_수_없다() {
		Long outsiderId = em.persist(Fixtures.user("out")).getId();

		assertThatThrownBy(() -> service.create(band.getId(), outsiderId, req()))
			.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void 목록은_참석자_집계와_내_출결을_담는다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();
		service.setAttendance(scheduleId, memberId, AttendanceStatus.ATTENDING);
		service.setAttendance(scheduleId, ownerId, AttendanceStatus.ABSENT);
		em.flush();
		em.clear();

		ScheduleResponse row = service.list(band.getId(), memberId).getFirst();

		assertThat(row.attendees()).hasSize(2);
		assertThat(row.counts().attending()).isEqualTo(1);
		assertThat(row.counts().absent()).isEqualTo(1);
		assertThat(row.myStatus()).isEqualTo(AttendanceStatus.ATTENDING);
	}

	@Test
	void 없는_밴드_목록은_404() {
		assertThatThrownBy(() -> service.list(999L, null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void 밴드_멤버는_남이_만든_일정도_수정할_수_있다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();

		ScheduleResponse updated = service.update(scheduleId, memberId,
				new ScheduleUpdateRequest(ScheduleType.PERFORMANCE, null, "홍대 공연장"));

		assertThat(updated.type()).isEqualTo(ScheduleType.PERFORMANCE);
		assertThat(updated.location()).isEqualTo("홍대 공연장");
		assertThat(updated.dateTime()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
	}

	@Test
	void 비멤버는_일정을_수정할_수_없다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Long outsiderId = em.persist(Fixtures.user("out2")).getId();
		em.flush();

		assertThatThrownBy(() -> service.update(scheduleId, outsiderId, new ScheduleUpdateRequest(null, null, "x")))
			.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void 관리자는_일정을_삭제하고_출결도_함께_사라진다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		service.setAttendance(scheduleId, memberId, AttendanceStatus.ATTENDING);
		em.flush();
		em.clear();

		service.delete(scheduleId, ownerId);
		em.flush();
		em.clear();

		assertThat(schedules.findById(scheduleId)).isEmpty();
		assertThat(attendances.findAllByScheduleId(scheduleId)).isEmpty();
	}

	@Test
	void 일반_멤버는_일정을_삭제할_수_없다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();

		assertThatThrownBy(() -> service.delete(scheduleId, memberId)).isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	@Test
	void 출결은_upsert_로_한_행만_유지된다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();

		service.setAttendance(scheduleId, memberId, AttendanceStatus.ATTENDING);
		ScheduleResponse afterChange = service.setAttendance(scheduleId, memberId, AttendanceStatus.ABSENT);
		em.flush();
		em.clear();

		assertThat(attendances.findAllByScheduleId(scheduleId)).hasSize(1);
		assertThat(afterChange.myStatus()).isEqualTo(AttendanceStatus.ABSENT);
	}

	@Test
	void 관리자는_다른_멤버의_출결을_대신_설정한다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		service.setAttendance(scheduleId, memberId, AttendanceStatus.UNDECIDED);
		em.flush();
		em.clear();

		service.setMemberAttendance(scheduleId, ownerId, memberId, AttendanceStatus.ABSENT);
		em.flush();
		em.clear();

		assertThat(attendances.findByScheduleIdAndUserId(scheduleId, memberId).orElseThrow().getStatus())
			.isEqualTo(AttendanceStatus.ABSENT);
		assertThat(attendances.findAllByScheduleId(scheduleId)).hasSize(1); // upsert, 중복
																			// 안 생김
	}

	@Test
	void 일반_멤버는_남의_출결을_설정할_수_없다_403() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();

		assertThatThrownBy(() -> service.setMemberAttendance(scheduleId, memberId, ownerId, AttendanceStatus.ATTENDING))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	@Test
	void 대상이_멤버가_아니면_404() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		em.flush();

		assertThatThrownBy(() -> service.setMemberAttendance(scheduleId, ownerId, 999_999L, AttendanceStatus.ATTENDING))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MEMBER_NOT_FOUND"));
	}

	// ── 게스트 참석 ───────────────────────────────────────

	@Test
	void 관리자는_게스트를_일정에_추가하고_제외한다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Long guestId = em.persist(Fixtures.guest(band, "세션 드럼")).getId();
		service.setAttendance(scheduleId, memberId, AttendanceStatus.ATTENDING);
		em.flush();
		em.clear();

		ScheduleResponse set = service.setGuestAttendance(scheduleId, ownerId, guestId);
		assertThat(set.attendees()).filteredOn(a -> a.guestId() != null)
			.singleElement()
			.satisfies(a -> assertThat(a.guestId()).isEqualTo(guestId),
					a -> assertThat(a.nickname()).isEqualTo("세션 드럼"), a -> assertThat(a.userId()).isNull(),
					a -> assertThat(a.status()).isEqualTo(AttendanceStatus.ATTENDING));
		// 게스트는 멤버 참석 집계에 포함되지 않는다
		assertThat(set.counts().attending()).isEqualTo(1);

		// 이미 있으면 중복 생성 안 함
		service.setGuestAttendance(scheduleId, ownerId, guestId);
		em.flush();
		em.clear();
		assertThat(attendances.findAllByScheduleId(scheduleId)).hasSize(2);

		ScheduleResponse removed = service.removeGuestAttendance(scheduleId, ownerId, guestId);
		assertThat(removed.attendees()).filteredOn(a -> a.guestId() != null).isEmpty();
	}

	@Test
	void 일반_멤버는_게스트를_일정에_추가할_수_없다_403() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Long guestId = em.persist(Fixtures.guest(band, "세션")).getId();
		em.flush();

		assertThatThrownBy(() -> service.setGuestAttendance(scheduleId, memberId, guestId))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	@Test
	void 다른_밴드_게스트는_404() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Band other = em.persist(Fixtures.band("B"));
		Long strayGuestId = em.persist(Fixtures.guest(other, "남")).getId();
		em.flush();

		assertThatThrownBy(() -> service.setGuestAttendance(scheduleId, ownerId, strayGuestId))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("GUEST_NOT_FOUND"));
	}

	@Test
	void 일정_목록에_공개범위_필터된_연결_영상이_포함된다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Schedule schedule = schedules.findById(scheduleId).orElseThrow();
		User uploader = users.findById(memberId).orElseThrow();
		media.save(Media.builder()
			.band(band)
			.schedule(schedule)
			.uploadedBy(uploader)
			.type(MediaType.REHEARSAL)
			.externalUrl("https://a/pub")
			.platform(MediaPlatform.OTHER)
			.visibility(MediaVisibility.LINK_PUBLIC)
			.build());
		media.save(Media.builder()
			.band(band)
			.schedule(schedule)
			.uploadedBy(uploader)
			.type(MediaType.REHEARSAL)
			.externalUrl("https://a/priv")
			.platform(MediaPlatform.OTHER)
			.visibility(MediaVisibility.MEMBERS_ONLY)
			.build());
		em.flush();
		em.clear();

		assertThat(service.list(band.getId(), memberId).getFirst().media()).hasSize(2);
		assertThat(service.list(band.getId(), null).getFirst().media()).extracting(MediaResponse::externalUrl)
			.containsExactly("https://a/pub");
	}

	@Test
	void 비멤버는_출결을_남길_수_없다() {
		Long scheduleId = service.create(band.getId(), ownerId, req()).id();
		Long outsiderId = em.persist(Fixtures.user("out3")).getId();
		em.flush();

		assertThatThrownBy(() -> service.setAttendance(scheduleId, outsiderId, AttendanceStatus.ATTENDING))
			.isInstanceOf(ForbiddenException.class);
	}

}
