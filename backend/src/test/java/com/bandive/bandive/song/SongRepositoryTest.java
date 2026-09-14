package com.bandive.bandive.song;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

class SongRepositoryTest extends RepositoryTest {

	@Autowired
	private SongRepository songs;

	@Autowired
	private SongPartRepository parts;

	@Autowired
	private TestEntityManager em;

	@Test
	void 곡을_저장하면_파트_슬롯이_cascade_로_함께_저장된다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Song song = Fixtures.song(band, user, SongStatus.WISHLIST);
		song.addPart(Fixtures.part("GUITAR", 1));
		song.addPart(Fixtures.part("GUITAR", 2));
		song.addPart(Fixtures.part("DRUM", 1));

		Song saved = songs.save(song);
		em.flush();
		em.clear();

		assertThat(parts.findAllBySongId(saved.getId())).hasSize(3);
	}

	@Test
	void orphanRemoval_로_파트를_컬렉션에서_빼면_삭제된다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Song song = Fixtures.song(band, user, SongStatus.WISHLIST);
		song.addPart(Fixtures.part("BASS", 1));
		song.addPart(Fixtures.part("VOCAL", 1));
		Song saved = songs.save(song);
		em.flush();

		saved.getParts().remove(0);
		em.flush();
		em.clear();

		assertThat(parts.findAllBySongId(saved.getId())).hasSize(1);
	}

	@Test
	void 밴드와_상태로_곡을_거른다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		songs.save(Fixtures.song(band, user, SongStatus.WISHLIST));
		songs.save(Fixtures.song(band, user, SongStatus.CONFIRMED));
		songs.save(Fixtures.song(band, user, SongStatus.CONFIRMED));
		em.flush();
		em.clear();

		List<Song> confirmed = songs.findAllByBandIdAndStatus(band.getId(), SongStatus.CONFIRMED);
		assertThat(confirmed).hasSize(2);
	}

}
