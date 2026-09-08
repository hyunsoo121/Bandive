package com.bandive.bandive.schedule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	@Query("""
			select a from Attendance a
			  left join fetch a.user
			  left join fetch a.guest
			where a.schedule.id = :scheduleId
			""")
	List<Attendance> findAllByScheduleId(Long scheduleId);

	Optional<Attendance> findByScheduleIdAndUserId(Long scheduleId, Long userId);

	Optional<Attendance> findByScheduleIdAndGuestId(Long scheduleId, Long guestId);

	/** 여러 일정의 출결을 user·guest fetch join 으로 한 번에. */
	@Query("""
			select a from Attendance a
			  left join fetch a.user
			  left join fetch a.guest
			where a.schedule.id in :scheduleIds
			""")
	List<Attendance> findAllByScheduleIds(Collection<Long> scheduleIds);

}
