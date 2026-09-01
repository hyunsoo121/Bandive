package com.bandive.bandive.schedule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	List<Attendance> findAllByScheduleId(Long scheduleId);

	Optional<Attendance> findByScheduleIdAndUserId(Long scheduleId, Long userId);

}
