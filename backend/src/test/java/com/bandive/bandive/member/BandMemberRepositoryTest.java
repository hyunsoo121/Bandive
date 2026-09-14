package com.bandive.bandive.member;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BandMemberRepositoryTest extends RepositoryTest {

	@Autowired
	private BandMemberRepository members;

	@Autowired
	private TestEntityManager em;

	@Test
	void 같은_밴드_같은_유저는_한_번만_가입된다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		members.save(Fixtures.member(band, user, BandRole.MEMBER));
		em.flush();

		assertThatThrownBy(() -> {
			members.save(Fixtures.member(band, user, BandRole.OWNER));
			em.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 소속_여부와_밴드별_유저별_조회() {
		Band band1 = em.persist(Fixtures.band("B1"));
		Band band2 = em.persist(Fixtures.band("B2"));
		User user = em.persist(Fixtures.user("u2"));
		members.save(Fixtures.member(band1, user, BandRole.OWNER));
		members.save(Fixtures.member(band2, user, BandRole.MEMBER));
		em.flush();
		em.clear();

		assertThat(members.existsByBandIdAndUserId(band1.getId(), user.getId())).isTrue();
		assertThat(members.existsByBandIdAndUserId(band2.getId(), 999L)).isFalse();
		assertThat(members.findAllByUserId(user.getId())).hasSize(2);
		assertThat(members.findByBandIdAndUserId(band1.getId(), user.getId())).get()
			.extracting(BandMember::getRole)
			.isEqualTo(BandRole.OWNER);
	}

}
