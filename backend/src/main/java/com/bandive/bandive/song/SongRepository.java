package com.bandive.bandive.song;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

	List<Song> findAllByBandId(Long bandId);

	List<Song> findAllByBandIdAndStatus(Long bandId, SongStatus status);

}
