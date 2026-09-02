package com.bandive.bandive.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	List<Schedule> findAllByBandIdOrderByDateTime(Long bandId);

	/** 목록 응답용 — createdBy 를 fetch join, dateTime 오름차순. */
	@Query("select s from Schedule s join fetch s.createdBy where s.band.id = :bandId order by s.dateTime asc")
	List<Schedule> findAllForBand(Long bandId);

}
