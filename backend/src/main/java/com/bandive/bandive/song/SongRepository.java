package com.bandive.bandive.song;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, Long> {

	List<Song> findAllByBandId(Long bandId);

	List<Song> findAllByBandIdAndStatus(Long bandId, SongStatus status);

	/** 목록 응답용 — addedBy·parts·배정멤버를 한 번에 fetch. status 가 null 이면 전체. */
	@Query("""
			select distinct s from Song s
			  join fetch s.addedBy
			  left join fetch s.parts p
			  left join fetch p.assignedMember m
			  left join fetch m.user
			where s.band.id = :bandId
			  and (:status is null or s.status = :status)
			""")
	List<Song> findAllForBand(Long bandId, SongStatus status);

	@Query("""
			select s from Song s
			  join fetch s.addedBy
			  left join fetch s.parts p
			  left join fetch p.assignedMember m
			  left join fetch m.user
			where s.id = :id
			""")
	Optional<Song> findByIdWithDetails(Long id);

}
