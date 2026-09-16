package com.bandive.bandive.follow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.security.BandAccessGuard;
import com.bandive.bandive.follow.BandFollow;
import com.bandive.bandive.follow.BandFollowRepository;
import com.bandive.bandive.follow.FollowStatus;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.notification.NotificationRepository;
import com.bandive.bandive.notification.service.NotificationService;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowServiceTest extends RepositoryTest {

	@Autowired
	private BandFollowRepository follows;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private UserRepository users;

	@Autowired
	private NotificationRepository notifications;

	@Autowired
	private TestEntityManager em;

	private FollowService service;

	private Band band;

	private Long ownerId;

	private Long outsiderId;

	@BeforeEach
	void setUp() {
		service = new FollowService(follows, bands, bandMembers, users,
				new BandAccessGuard(bands, bandMembers, follows), new NotificationService(notifications, bandMembers));
		band = em.persist(Fixtures.band("A", BandVisibility.FOLLOWERS));
		User owner = em.persist(Fixtures.user("owner"));
		em.persist(Fixtures.member(band, owner, BandRole.OWNER));
		ownerId = owner.getId();
		outsiderId = em.persist(Fixtures.user("out")).getId();
	}

	@Test
	void 요청은_PENDING_이고_멱등이다() {
		service.request(band.getId(), outsiderId);
		service.request(band.getId(), outsiderId);
		em.flush();

		assertThat(follows.findAllForBand(band.getId(), null)).singleElement()
			.satisfies(f -> assertThat(f.getStatus()).isEqualTo(FollowStatus.PENDING));
	}

	@Test
	void PRIVATE_밴드엔_요청_불가_409() {
		band.changeVisibility(BandVisibility.PRIVATE);
		em.flush();

		assertThatThrownBy(() -> service.request(band.getId(), outsiderId)).isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("FOLLOW_NOT_AVAILABLE"));
	}

	@Test
	void PUBLIC_밴드는_요청하면_즉시_APPROVED_된다() {
		band.changeVisibility(BandVisibility.PUBLIC);
		em.flush();

		service.request(band.getId(), outsiderId);
		em.flush();
		em.clear();

		BandFollow follow = follows.findByBandIdAndUserId(band.getId(), outsiderId).orElseThrow();
		assertThat(follow.getStatus()).isEqualTo(FollowStatus.APPROVED);
		assertThat(follow.getDecidedAt()).isNotNull();
	}

	@Test
	void 팔로우_요청하면_관리자에게_알림이_간다() {
		service.request(band.getId(), outsiderId);
		em.flush();
		em.clear();

		assertThat(notifications.findRecentByRecipient(ownerId, org.springframework.data.domain.Limit.of(10)))
			.singleElement()
			.satisfies(n -> {
				assertThat(n.getType()).isEqualTo(com.bandive.bandive.notification.NotificationType.FOLLOW_REQUESTED);
				assertThat(n.getActor().getId()).isEqualTo(outsiderId);
				assertThat(n.getBand().getId()).isEqualTo(band.getId());
			});
	}

	@Test
	void PUBLIC_밴드_즉시승인도_관리자에게_확인용_알림이_간다() {
		band.changeVisibility(BandVisibility.PUBLIC);
		em.flush();

		service.request(band.getId(), outsiderId);
		em.flush();
		em.clear();

		assertThat(notifications.findRecentByRecipient(ownerId, org.springframework.data.domain.Limit.of(10)))
			.singleElement()
			.satisfies(n -> assertThat(n.getType())
				.isEqualTo(com.bandive.bandive.notification.NotificationType.FOLLOW_AUTO_APPROVED));
	}

	@Test
	void 멤버는_팔로우_요청_불가_409() {
		assertThatThrownBy(() -> service.request(band.getId(), ownerId)).isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("ALREADY_MEMBER"));
	}

	@Test
	void 관리자가_승인하면_APPROVED_이고_게이트를_통과한다() {
		service.request(band.getId(), outsiderId);
		em.flush();

		service.approve(band.getId(), ownerId, outsiderId);
		em.flush();
		em.clear();

		assertThat(follows.findByBandIdAndUserId(band.getId(), outsiderId).orElseThrow().getStatus())
			.isEqualTo(FollowStatus.APPROVED);

		BandAccessGuard guard = new BandAccessGuard(bands, bandMembers, follows);
		assertThat(guard.canViewContent(bands.findById(band.getId()).orElseThrow(), outsiderId)).isTrue();
	}

	@Test
	void 관리자가_아니면_목록_승인_불가_403() {
		Long plainMemberId = em.persist(Fixtures.user("mem")).getId();
		em.persist(Fixtures.member(band, em.find(User.class, plainMemberId), BandRole.MEMBER));
		service.request(band.getId(), outsiderId);
		em.flush();

		// 비멤버 → NOT_A_MEMBER
		assertThatThrownBy(() -> service.listFollowers(band.getId(), outsiderId, null))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_A_MEMBER"));
		// 일반 멤버 → NOT_BAND_OWNER
		assertThatThrownBy(() -> service.approve(band.getId(), plainMemberId, outsiderId))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	@Test
	void 공개_팔로워_목록은_승인된_사람만_담고_콘텐츠_열람_가능해야_조회된다() {
		service.request(band.getId(), outsiderId);
		em.flush();
		service.approve(band.getId(), ownerId, outsiderId);
		em.flush();

		Long pendingId = em.persist(Fixtures.user("pending")).getId();
		service.request(band.getId(), pendingId);
		em.flush();
		em.clear();

		// FOLLOWERS 밴드 콘텐츠를 볼 수 있는 사람(자기 자신, 승인된 팔로워) → 조회 가능, 대기중인 사람은 목록에서 제외
		var list = service.listPublicFollowers(band.getId(), outsiderId);

		assertThat(list).singleElement().satisfies(f -> assertThat(f.userId()).isEqualTo(outsiderId));
	}

	@Test
	void FOLLOWERS_밴드는_콘텐츠_못보는_사람에게_공개_팔로워_목록도_403() {
		assertThatThrownBy(() -> service.listPublicFollowers(band.getId(), outsiderId))
			.isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("CONTENT_RESTRICTED"));
	}

	@Test
	void PUBLIC_밴드는_비로그인도_공개_팔로워_목록을_본다() {
		band.changeVisibility(BandVisibility.PUBLIC);
		em.flush();
		service.request(band.getId(), outsiderId);
		em.flush();
		em.clear();

		var list = service.listPublicFollowers(band.getId(), null);

		assertThat(list).singleElement().satisfies(f -> assertThat(f.userId()).isEqualTo(outsiderId));
	}

	@Test
	void 승인할_요청이_없으면_404() {
		assertThatThrownBy(() -> service.approve(band.getId(), ownerId, outsiderId))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("FOLLOW_REQUEST_NOT_FOUND"));
	}

	@Test
	void 내가_팔로우한_밴드를_상태와_멤버수와_함께_준다() {
		Band other = em.persist(Fixtures.band("B", BandVisibility.FOLLOWERS));
		em.persist(Fixtures.member(other, em.find(User.class, ownerId), BandRole.OWNER));

		service.request(band.getId(), outsiderId);
		service.request(other.getId(), outsiderId);
		em.flush();
		service.approve(other.getId(), ownerId, outsiderId);
		em.flush();
		em.clear();

		var mine = service.listMyFollowing(outsiderId);

		assertThat(mine).hasSize(2);
		assertThat(mine).filteredOn(f -> f.bandId().equals(other.getId())).singleElement().satisfies(f -> {
			assertThat(f.status()).isEqualTo(FollowStatus.APPROVED);
			assertThat(f.memberCount()).isEqualTo(1);
		});
		assertThat(mine).filteredOn(f -> f.bandId().equals(band.getId()))
			.singleElement()
			.satisfies(f -> assertThat(f.status()).isEqualTo(FollowStatus.PENDING));
	}

	@Test
	void 취소_거절하면_행이_사라진다() {
		service.request(band.getId(), outsiderId);
		em.flush();
		service.cancel(band.getId(), outsiderId);
		em.flush();
		assertThat(follows.findByBandIdAndUserId(band.getId(), outsiderId)).isEmpty();

		service.request(band.getId(), outsiderId);
		em.flush();
		service.remove(band.getId(), ownerId, outsiderId);
		em.flush();
		assertThat(follows.findByBandIdAndUserId(band.getId(), outsiderId)).isEmpty();
	}

}
