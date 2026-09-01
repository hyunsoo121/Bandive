package com.bandive.bandive.song;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

	boolean existsBySongIdAndUserId(Long songId, Long userId);

	long countBySongId(Long songId);

	void deleteBySongIdAndUserId(Long songId, Long userId);

}
