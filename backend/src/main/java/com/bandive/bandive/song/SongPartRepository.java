package com.bandive.bandive.song;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SongPartRepository extends JpaRepository<SongPart, Long> {

	List<SongPart> findAllBySongId(Long songId);

	Optional<SongPart> findByIdAndSongId(Long id, Long songId);

}
