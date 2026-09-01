package com.bandive.bandive.media;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {

	List<Media> findAllByBandId(Long bandId);

	List<Media> findAllByBandIdAndScheduleId(Long bandId, Long scheduleId);

}
