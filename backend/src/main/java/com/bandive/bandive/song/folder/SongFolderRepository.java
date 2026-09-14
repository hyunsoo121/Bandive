package com.bandive.bandive.song.folder;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bandive.bandive.song.SongStatus;

public interface SongFolderRepository extends JpaRepository<SongFolder, Long> {

	List<SongFolder> findByBandIdOrderByStatusAscPositionAsc(Long bandId);

	List<SongFolder> findByBandIdAndStatusOrderByPositionAsc(Long bandId, SongStatus status);

	int countByBandIdAndStatus(Long bandId, SongStatus status);

}
