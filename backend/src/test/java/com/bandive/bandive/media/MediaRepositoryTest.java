package com.bandive.bandive.media;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

class MediaRepositoryTest extends RepositoryTest {

	@Autowired
	private MediaRepository media;

	@Autowired
	private TestEntityManager em;

	@Test
	void 일정_연결은_선택적이고_밴드_일정별로_조회한다() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Schedule schedule = em.persist(Fixtures.schedule(band, user));

		media.save(base(band, user).schedule(schedule).build());
		media.save(base(band, user).schedule(null).build());
		em.flush();
		em.clear();

		assertThat(media.findAllByBandId(band.getId())).hasSize(2);
		assertThat(media.findAllByBandIdAndScheduleId(band.getId(), schedule.getId())).hasSize(1);
	}

	private Media.MediaBuilder base(Band band, User user) {
		return Media.builder()
			.band(band)
			.uploadedBy(user)
			.type(MediaType.REHEARSAL)
			.platform(MediaPlatform.YOUTUBE)
			.visibility(MediaVisibility.MEMBERS_ONLY)
			.externalUrl("https://youtu.be/abc");
	}

}
