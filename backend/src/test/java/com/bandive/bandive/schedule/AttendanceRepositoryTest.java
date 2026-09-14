package com.bandive.bandive.schedule;

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

class AttendanceRepositoryTest extends RepositoryTest {

	@Autowired
	private AttendanceRepository attendances;

	@Autowired
	private TestEntityManager em;

	@Test
	void 일정당_유저당_출결은_하나() {
		Band band = em.persist(Fixtures.band("A"));
		User user = em.persist(Fixtures.user("u1"));
		Schedule schedule = em.persist(Fixtures.schedule(band, user));
		attendances.save(Attendance.builder().schedule(schedule).user(user).status(AttendanceStatus.ATTENDING).build());
		em.flush();

		assertThatThrownBy(() -> {
			attendances
				.save(Attendance.builder().schedule(schedule).user(user).status(AttendanceStatus.ABSENT).build());
			em.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 일정별_출결_조회() {
		Band band = em.persist(Fixtures.band("A"));
		User u1 = em.persist(Fixtures.user("u1"));
		User u2 = em.persist(Fixtures.user("u2"));
		Schedule schedule = em.persist(Fixtures.schedule(band, u1));
		attendances.save(Attendance.builder().schedule(schedule).user(u1).status(AttendanceStatus.ATTENDING).build());
		attendances.save(Attendance.builder().schedule(schedule).user(u2).status(AttendanceStatus.UNDECIDED).build());
		em.flush();
		em.clear();

		assertThat(attendances.findAllByScheduleId(schedule.getId())).hasSize(2);
		assertThat(attendances.findByScheduleIdAndUserId(schedule.getId(), u2.getId())).get()
			.extracting(Attendance::getStatus)
			.isEqualTo(AttendanceStatus.UNDECIDED);
	}

}
