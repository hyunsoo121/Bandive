package com.bandive.bandive.song;

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

class VoteRepositoryTest extends RepositoryTest {

	@Autowired
	private VoteRepository votes;

	@Autowired
	private TestEntityManager em;

	@Test
	void 곡당_유저당_한_표만_허용된다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Song song = em.persist(Fixtures.song(band, user, SongStatus.WISHLIST));
		votes.save(Vote.builder().song(song).user(user).build());
		em.flush();

		assertThatThrownBy(() -> {
			votes.save(Vote.builder().song(song).user(user).build());
			em.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 곡별_득표수_집계와_취소() {
		Band band = em.persist(Fixtures.band("A"));
		User u1 = em.persist(Fixtures.user("u1"));
		User u2 = em.persist(Fixtures.user("u2"));
		Song song = em.persist(Fixtures.song(band, u1, SongStatus.WISHLIST));
		votes.save(Vote.builder().song(song).user(u1).build());
		votes.save(Vote.builder().song(song).user(u2).build());
		em.flush();

		assertThat(votes.countBySongId(song.getId())).isEqualTo(2);
		assertThat(votes.existsBySongIdAndUserId(song.getId(), u1.getId())).isTrue();

		votes.deleteBySongIdAndUserId(song.getId(), u1.getId());
		em.flush();

		assertThat(votes.countBySongId(song.getId())).isEqualTo(1);
		assertThat(votes.existsBySongIdAndUserId(song.getId(), u1.getId())).isFalse();
	}

}
