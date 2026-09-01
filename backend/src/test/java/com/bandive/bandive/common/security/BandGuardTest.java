package com.bandive.bandive.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.band.Band;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class BandGuardTest extends RepositoryTest {

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private TestEntityManager em;

	private BandGuard guard() {
		return new BandGuard(bandMembers);
	}

	private void login(Long userId) {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, null));
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void 밴드장은_isOwner_와_isMember_모두_참() {
		Band band = em.persist(Fixtures.band("A"));
		User owner = em.persist(Fixtures.user("owner"));
		bandMembers.save(Fixtures.member(band, owner, BandRole.OWNER));
		em.flush();
		login(owner.getId());

		assertThat(guard().isOwner(band.getId())).isTrue();
		assertThat(guard().isMember(band.getId())).isTrue();
	}

	@Test
	void 일반_멤버는_isMember_만_참() {
		Band band = em.persist(Fixtures.band("A"));
		User member = em.persist(Fixtures.user("member"));
		bandMembers.save(Fixtures.member(band, member, BandRole.MEMBER));
		em.flush();
		login(member.getId());

		assertThat(guard().isMember(band.getId())).isTrue();
		assertThat(guard().isOwner(band.getId())).isFalse();
	}

	@Test
	void 비소속_유저는_둘_다_거짓() {
		Band band = em.persist(Fixtures.band("A"));
		em.flush();
		login(999L);

		assertThat(guard().isMember(band.getId())).isFalse();
		assertThat(guard().isOwner(band.getId())).isFalse();
	}

	@Test
	void 미인증이면_거짓() {
		Band band = em.persist(Fixtures.band("A"));
		em.flush();

		assertThat(guard().isMember(band.getId())).isFalse();
	}

}
