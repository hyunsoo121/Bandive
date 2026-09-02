package com.bandive.bandive.song;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongPartRepositoryTest extends RepositoryTest {

	@Autowired
	private SongPartRepository parts;

	@Autowired
	private TestEntityManager em;

	@Test
	void 같은_곡_같은_악기_같은_순번_슬롯은_중복될_수_없다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Song song = em.persist(Fixtures.song(band, user, SongStatus.WISHLIST));

		parts.save(SongPart.builder().song(song).instrument("KEYBOARD").partIndex(1).build());
		em.flush();

		assertThatThrownBy(() -> {
			parts.save(SongPart.builder().song(song).instrument("KEYBOARD").partIndex(1).build());
			em.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
