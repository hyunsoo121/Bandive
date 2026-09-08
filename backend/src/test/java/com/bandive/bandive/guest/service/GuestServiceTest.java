package com.bandive.bandive.guest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.guest.GuestRepository;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestServiceTest extends RepositoryTest {

	@Autowired
	private GuestRepository guests;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private TestEntityManager em;

	private GuestService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new GuestService(guests, bands, bandMembers);
		band = em.persist(Fixtures.band("A"));
		ownerId = join("owner", BandRole.OWNER);
		memberId = join("member", BandRole.MEMBER);
	}

	private Long join(String kakaoId, BandRole role) {
		User user = em.persist(Fixtures.user(kakaoId));
		em.persist(Fixtures.member(band, user, role));
		return user.getId();
	}

	@Test
	void 관리자만_게스트를_등록한다() {
		assertThatThrownBy(() -> service.create(band.getId(), memberId, "세션 기타")).isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));

		var created = service.create(band.getId(), ownerId, "  세션 기타  ");
		assertThat(created.name()).isEqualTo("세션 기타");
		assertThat(created.bandId()).isEqualTo(band.getId());
	}

	@Test
	void 같은_이름_게스트는_등록_불가_409() {
		service.create(band.getId(), ownerId, "세션 기타");
		em.flush();

		assertThatThrownBy(() -> service.create(band.getId(), ownerId, "  세션 기타  "))
			.isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("GUEST_NAME_TAKEN"));

		// 다른 밴드에는 같은 이름 허용
		Band other = em.persist(Fixtures.band("B"));
		em.persist(Fixtures.member(other, em.persist(Fixtures.user("o2")), BandRole.OWNER));
		em.flush();
	}

	@Test
	void 이름_변경도_중복이면_409() {
		service.create(band.getId(), ownerId, "가");
		Long bId = service.create(band.getId(), ownerId, "나").id();
		em.flush();

		assertThatThrownBy(() -> service.rename(band.getId(), bId, ownerId, "가")).isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("GUEST_NAME_TAKEN"));
		// 자기 이름 그대로 두는 건 OK
		assertThat(service.rename(band.getId(), bId, ownerId, "나").name()).isEqualTo("나");
	}

	@Test
	void 목록은_이름순_공개() {
		service.create(band.getId(), ownerId, "다");
		service.create(band.getId(), ownerId, "가");
		service.create(band.getId(), ownerId, "나");
		em.flush();

		assertThat(service.list(band.getId())).extracting(g -> g.name()).containsExactly("가", "나", "다");
	}

	@Test
	void 세션_설정은_관리자만_이고_빈값이면_지운다() {
		Long guestId = service.create(band.getId(), ownerId, "세션맨").id();
		em.flush();

		assertThatThrownBy(() -> service.setSession(band.getId(), guestId, memberId, "드럼"))
			.isInstanceOf(ForbiddenException.class);

		assertThat(service.setSession(band.getId(), guestId, ownerId, "  드럼  ").session()).isEqualTo("드럼");
		assertThat(service.setSession(band.getId(), guestId, ownerId, "관객").session()).isEqualTo("관객");
		assertThat(service.setSession(band.getId(), guestId, ownerId, "   ").session()).isNull();
		assertThat(service.setSession(band.getId(), guestId, ownerId, null).session()).isNull();
	}

	@Test
	void 이름_수정_삭제는_관리자만() {
		Long guestId = service.create(band.getId(), ownerId, "임시").id();
		em.flush();

		assertThatThrownBy(() -> service.rename(band.getId(), guestId, memberId, "새이름"))
			.isInstanceOf(ForbiddenException.class);
		assertThat(service.rename(band.getId(), guestId, ownerId, "정식 게스트").name()).isEqualTo("정식 게스트");

		assertThatThrownBy(() -> service.delete(band.getId(), guestId, memberId))
			.isInstanceOf(ForbiddenException.class);
		service.delete(band.getId(), guestId, ownerId);
		em.flush();
		assertThat(guests.findById(guestId)).isEmpty();
	}

	@Test
	void 다른_밴드_게스트는_찾지_못한다() {
		Band other = em.persist(Fixtures.band("B"));
		Long strayId = em.persist(Fixtures.guest(other, "남")).getId();
		em.flush();

		assertThatThrownBy(() -> service.rename(band.getId(), strayId, ownerId, "x"))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("GUEST_NOT_FOUND"));
	}

}
