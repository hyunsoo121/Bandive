package com.bandive.bandive.member.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.member.dto.MemberPartsRequest;
import com.bandive.bandive.member.dto.MemberResponse;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberServiceTest extends RepositoryTest {

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private TestEntityManager em;

	private MemberService service;

	private Band band;

	@BeforeEach
	void setUp() {
		service = new MemberService(bands, bandMembers);
		band = em.persist(Fixtures.band("A"));
	}

	private BandMember join(String kakaoId, BandRole role, String joinedAt) {
		User user = em.persist(Fixtures.user(kakaoId));
		return em
			.persist(BandMember.builder().band(band).user(user).role(role).joinedAt(Instant.parse(joinedAt)).build());
	}

	@Test
	void 목록은_OWNER_먼저_가입순으로_정렬된다() {
		join("late", BandRole.MEMBER, "2026-09-03T00:00:00Z");
		join("owner", BandRole.OWNER, "2026-09-05T00:00:00Z");
		join("early", BandRole.MEMBER, "2026-09-01T00:00:00Z");
		em.flush();
		em.clear();

		List<MemberResponse> members = service.list(band.getId());

		assertThat(members).extracting(MemberResponse::nickname)
			.containsExactly("nick-owner", "nick-early", "nick-late");
	}

	@Test
	void 없는_밴드_목록은_404() {
		assertThatThrownBy(() -> service.list(999L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void 내_파트를_설정하고_해제한다() {
		BandMember me = join("me", BandRole.MEMBER, "2026-09-01T00:00:00Z");
		Long myUserId = me.getUser().getId();
		em.flush();

		MemberResponse set = service.updateMyParts(band.getId(), myUserId,
				new MemberPartsRequest(List.of("GUITAR", "VOCAL", " GUITAR ")));
		assertThat(set.parts()).containsExactly("GUITAR", "VOCAL");

		MemberResponse cleared = service.updateMyParts(band.getId(), myUserId, new MemberPartsRequest(null));
		assertThat(cleared.parts()).isEmpty();
	}

	@Test
	void 비멤버가_내_파트_설정하면_404() {
		assertThatThrownBy(() -> service.updateMyParts(band.getId(), 999L, new MemberPartsRequest(List.of("DRUM"))))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("NOT_A_MEMBER"));
	}

	@Test
	void 밴드장이_남의_파트를_설정한다() {
		join("owner", BandRole.OWNER, "2026-09-01T00:00:00Z");
		BandMember target = join("m", BandRole.MEMBER, "2026-09-02T00:00:00Z");
		em.flush();

		MemberResponse updated = service.updateMemberParts(band.getId(), target.getUser().getId(),
				new MemberPartsRequest(List.of("BASS")));

		assertThat(updated.parts()).containsExactly("BASS");
	}

	@Test
	void 추방하면_멤버_행이_삭제된다() {
		join("owner", BandRole.OWNER, "2026-09-01T00:00:00Z");
		BandMember target = join("m", BandRole.MEMBER, "2026-09-02T00:00:00Z");
		Long targetUserId = target.getUser().getId();
		em.flush();

		service.kick(band.getId(), targetUserId);
		em.flush();

		assertThat(bandMembers.existsByBandIdAndUserId(band.getId(), targetUserId)).isFalse();
	}

	@Test
	void 밴드장은_추방할_수_없다_409() {
		BandMember owner = join("owner", BandRole.OWNER, "2026-09-01T00:00:00Z");
		em.flush();

		assertThatThrownBy(() -> service.kick(band.getId(), owner.getUser().getId()))
			.isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("CANNOT_KICK_OWNER"));
	}

	@Test
	void 없는_멤버_추방은_404() {
		assertThatThrownBy(() -> service.kick(band.getId(), 999L)).isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MEMBER_NOT_FOUND"));
	}

	@Test
	void 탈퇴하면_본인_행이_삭제된다() {
		join("owner", BandRole.OWNER, "2026-09-01T00:00:00Z");
		BandMember me = join("me", BandRole.MEMBER, "2026-09-02T00:00:00Z");
		Long myUserId = me.getUser().getId();
		em.flush();

		service.leave(band.getId(), myUserId);
		em.flush();

		assertThat(bandMembers.existsByBandIdAndUserId(band.getId(), myUserId)).isFalse();
	}

	@Test
	void 밴드장은_탈퇴할_수_없다_409() {
		BandMember owner = join("owner", BandRole.OWNER, "2026-09-01T00:00:00Z");
		em.flush();

		assertThatThrownBy(() -> service.leave(band.getId(), owner.getUser().getId()))
			.isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("OWNER_CANNOT_LEAVE"));
	}

	@Test
	void 멤버가_아니면_탈퇴는_404() {
		assertThatThrownBy(() -> service.leave(band.getId(), 999L)).isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("NOT_A_MEMBER"));
	}

}
