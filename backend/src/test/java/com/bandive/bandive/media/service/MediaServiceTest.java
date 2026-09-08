package com.bandive.bandive.media.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaRepository;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.media.dto.MediaCreateRequest;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.media.dto.MediaUpdateRequest;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.schedule.ScheduleRepository;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaServiceTest extends RepositoryTest {

	@Autowired
	private MediaRepository media;

	@Autowired
	private ScheduleRepository schedules;

	@Autowired
	private BandRepository bands;

	@Autowired
	private com.bandive.bandive.member.BandMemberRepository bandMembers;

	@Autowired
	private UserRepository users;

	@Autowired
	private TestEntityManager em;

	private MediaService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new MediaService(media, schedules, bands, bandMembers, users);
		band = em.persist(Fixtures.band("A"));
		ownerId = joinMember("owner", BandRole.OWNER);
		memberId = joinMember("member", BandRole.MEMBER);
	}

	private Long joinMember(String kakaoId, BandRole role) {
		User user = em.persist(Fixtures.user(kakaoId));
		em.persist(Fixtures.member(band, user, role));
		return user.getId();
	}

	private MediaCreateRequest req(String url, MediaVisibility visibility, Long scheduleId) {
		return new MediaCreateRequest(url, MediaType.REHEARSAL, visibility, null, scheduleId);
	}

	@Test
	void 등록하면_URL_로_플랫폼을_판별하고_기본_공개범위는_MEMBERS_ONLY() {
		MediaResponse created = service.create(band.getId(), memberId, req("https://youtu.be/abc", null, null));

		assertThat(created.platform()).isEqualTo(MediaPlatform.YOUTUBE);
		assertThat(created.visibility()).isEqualTo(MediaVisibility.MEMBERS_ONLY);
		assertThat(created.uploadedByUserId()).isEqualTo(memberId);
	}

	@Test
	void 비멤버는_영상을_등록할_수_없다() {
		Long outsiderId = em.persist(Fixtures.user("out")).getId();

		assertThatThrownBy(() -> service.create(band.getId(), outsiderId, req("https://youtu.be/x", null, null)))
			.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void 다른_밴드의_일정에는_연결할_수_없다() {
		Band otherBand = em.persist(Fixtures.band("B"));
		User otherUser = em.persist(Fixtures.user("b-owner"));
		Schedule otherSchedule = em.persist(Fixtures.schedule(otherBand, otherUser));
		em.flush();

		assertThatThrownBy(
				() -> service.create(band.getId(), memberId, req("https://youtu.be/x", null, otherSchedule.getId())))
			.isInstanceOf(ValidationException.class)
			.satisfies(ex -> assertThat(((ValidationException) ex).getCode()).isEqualTo("SCHEDULE_BAND_MISMATCH"));
	}

	@Test
	void 목록은_공개범위로_거른다() {
		User owner = users.findById(ownerId).orElseThrow();
		service.create(band.getId(), ownerId, req("https://a.com/1", MediaVisibility.MEMBERS_ONLY, null));
		service.create(band.getId(), ownerId, req("https://b.com/2", MediaVisibility.LINK_PUBLIC, null));
		em.flush();
		em.clear();

		assertThat(service.list(band.getId(), null, memberId)).hasSize(2);
		assertThat(service.list(band.getId(), null, null)).hasSize(1);
		Long outsiderId = em.persist(Fixtures.user("out2")).getId();
		assertThat(service.list(band.getId(), null, outsiderId)).hasSize(1);
	}

	@Test
	void scheduleId_로_거른다() {
		Schedule schedule = em.persist(Fixtures.schedule(band, users.findById(ownerId).orElseThrow()));
		em.flush();
		service.create(band.getId(), ownerId,
				req("https://a.com/linked", MediaVisibility.LINK_PUBLIC, schedule.getId()));
		service.create(band.getId(), ownerId, req("https://a.com/free", MediaVisibility.LINK_PUBLIC, null));
		em.flush();
		em.clear();

		assertThat(service.list(band.getId(), schedule.getId(), memberId)).hasSize(1);
	}

	@Test
	void 공개범위_변경은_등록자_본인_또는_관리자() {
		Long mediaId = service
			.create(band.getId(), memberId, req("https://a.com/x", MediaVisibility.MEMBERS_ONLY, null))
			.id();
		Long thirdId = joinMember("third", BandRole.MEMBER);
		em.flush();

		assertThat(service.changeVisibility(mediaId, memberId, MediaVisibility.LINK_PUBLIC).visibility())
			.isEqualTo(MediaVisibility.LINK_PUBLIC); // 등록자 본인
		assertThat(service.changeVisibility(mediaId, ownerId, MediaVisibility.MEMBERS_ONLY).visibility())
			.isEqualTo(MediaVisibility.MEMBERS_ONLY); // 관리자

		assertThatThrownBy(() -> service.changeVisibility(mediaId, thirdId, MediaVisibility.LINK_PUBLIC))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	@Test
	void 부분수정_null_필드는_유지하고_URL_바뀌면_플랫폼_재판별() {
		Long mediaId = service
			.create(band.getId(), memberId,
					new MediaCreateRequest("https://youtu.be/abc", MediaType.REHEARSAL, MediaVisibility.MEMBERS_ONLY,
							"원래 제목", null))
			.id();
		em.flush();

		MediaResponse r = service.update(mediaId, memberId,
				new MediaUpdateRequest("https://drive.google.com/file/d/xyz/view", null, null, null, null));

		assertThat(r.externalUrl()).isEqualTo("https://drive.google.com/file/d/xyz/view");
		assertThat(r.platform()).isEqualTo(MediaPlatform.GOOGLE_DRIVE); // URL 바뀌어 재판별
		assertThat(r.type()).isEqualTo(MediaType.REHEARSAL); // 유지
		assertThat(r.visibility()).isEqualTo(MediaVisibility.MEMBERS_ONLY); // 유지
		assertThat(r.title()).isEqualTo("원래 제목"); // 유지
	}

	@Test
	void 부분수정은_등록자가_아니면_관리자여야() {
		Long mediaId = service.create(band.getId(), memberId, req("https://a.com/x", MediaVisibility.LINK_PUBLIC, null))
			.id();
		Long thirdId = joinMember("third", BandRole.MEMBER);
		em.flush();

		assertThat(service.update(mediaId, ownerId, new MediaUpdateRequest(null, null, null, "관리자가 고침", null)).title())
			.isEqualTo("관리자가 고침");
		assertThatThrownBy(
				() -> service.update(mediaId, thirdId, new MediaUpdateRequest(null, null, null, "남이 고침", null)))
			.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void 삭제는_등록자_본인_또는_관리자() {
		Long mine = service.create(band.getId(), memberId, req("https://a.com/mine", MediaVisibility.LINK_PUBLIC, null))
			.id();
		Long ownersUpload = service
			.create(band.getId(), ownerId, req("https://a.com/owner", MediaVisibility.LINK_PUBLIC, null))
			.id();
		Long thirdId = joinMember("third", BandRole.MEMBER);
		em.flush();

		service.delete(mine, memberId); // 등록자 본인
		service.delete(ownersUpload, ownerId); // 관리자가 자기 것
		em.flush();
		assertThat(media.findById(mine)).isEmpty();

		Long another = service.create(band.getId(), memberId, req("https://a.com/y", MediaVisibility.LINK_PUBLIC, null))
			.id();
		em.flush();
		assertThatThrownBy(() -> service.delete(another, thirdId)).isInstanceOf(ForbiddenException.class);
	}

}
