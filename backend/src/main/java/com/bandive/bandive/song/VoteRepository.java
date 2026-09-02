package com.bandive.bandive.song;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VoteRepository extends JpaRepository<Vote, Long> {

	boolean existsBySongIdAndUserId(Long songId, Long userId);

	long countBySongId(Long songId);

	void deleteBySongIdAndUserId(Long songId, Long userId);

	/** [songId, count] 행들 — 목록 응답의 득표수 배치 조회. */
	@Query("select v.song.id, count(v) from Vote v where v.song.id in :songIds group by v.song.id")
	List<Object[]> countBySongIds(Collection<Long> songIds);

	@Query("select v.song.id from Vote v where v.user.id = :userId and v.song.id in :songIds")
	List<Long> findVotedSongIds(Long userId, Collection<Long> songIds);

}
