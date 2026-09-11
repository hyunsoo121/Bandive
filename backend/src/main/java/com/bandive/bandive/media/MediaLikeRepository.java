package com.bandive.bandive.media;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaLikeRepository extends JpaRepository<MediaLike, Long> {

	boolean existsByMediaIdAndUserId(Long mediaId, Long userId);

	long countByMediaId(Long mediaId);

	void deleteByMediaIdAndUserId(Long mediaId, Long userId);

	/** [mediaId, count] 행들 — 목록 응답의 좋아요 수 배치 조회(N+1 방지). */
	@Query("select l.media.id, count(l) from MediaLike l where l.media.id in :mediaIds group by l.media.id")
	List<Object[]> countByMediaIds(Collection<Long> mediaIds);

	@Query("select l.media.id from MediaLike l where l.user.id = :userId and l.media.id in :mediaIds")
	List<Long> findLikedMediaIds(Long userId, Collection<Long> mediaIds);

}
