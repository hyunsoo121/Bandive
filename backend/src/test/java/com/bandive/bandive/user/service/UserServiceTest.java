package com.bandive.bandive.user.service;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;
import com.bandive.bandive.user.dto.UserProfileResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest extends RepositoryTest {

	@Autowired
	private UserRepository users;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private TestEntityManager em;

	private UserService service;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		service = new UserService(users, bandMembers);
	}

	@Test
	void 프로필은_닉네임_한줄소개_소속밴드를_담는다() {
		User user = em.persist(Fixtures.user("k-1"));
		user.updateBio("메인 보컬 3년차");
		Band a = em.persist(Fixtures.band("A"));
		Band b = em.persist(Fixtures.band("B"));
		em.persist(Fixtures.member(a, user, BandRole.OWNER));
		em.persist(Fixtures.member(b, user, BandRole.MEMBER));
		em.persist(Fixtures.member(a, em.persist(Fixtures.user("k-2")), BandRole.MEMBER));
		em.flush();
		em.clear();

		UserProfileResponse profile = service.getProfile(user.getId());

		assertThat(profile.nickname()).isEqualTo("nick-k-1");
		assertThat(profile.bio()).isEqualTo("메인 보컬 3년차");
		assertThat(profile.bands()).extracting(UserProfileResponse.BandBrief::name).containsExactly("A", "B");
		assertThat(profile.bands().get(0).role()).isEqualTo(BandRole.OWNER);
		assertThat(profile.bands().get(0).memberCount()).isEqualTo(2);
	}

	@Test
	void 없는_유저는_404() {
		assertThatThrownBy(() -> service.getProfile(999_999L)).isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("USER_NOT_FOUND"));
	}

}
