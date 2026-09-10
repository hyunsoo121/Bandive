package com.bandive.bandive.follow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BandFollowRepository extends JpaRepository<BandFollow, Long> {

	Optional<BandFollow> findByBandIdAndUserId(Long bandId, Long userId);

	boolean existsByBandIdAndUserIdAndStatus(Long bandId, Long userId, FollowStatus status);

	long countByBandIdAndStatus(Long bandId, FollowStatus status);

	/** 관리자용 팔로워 목록 — user fetch join, 요청/승인 순. status null 이면 전체. */
	@Query("""
			select f from BandFollow f
			  join fetch f.user
			where f.band.id = :bandId
			  and (:status is null or f.status = :status)
			order by f.createdAt asc
			""")
	List<BandFollow> findAllForBand(Long bandId, FollowStatus status);

	/**
	 * 내가 팔로우한 밴드 — {@code [Band, FollowStatus, Instant createdAt, long memberCount]}, 최근
	 * 요청 순.
	 */
	@Query("""
			select f.band, f.status, f.createdAt,
			       (select count(m.id) from BandMember m where m.band.id = f.band.id)
			from BandFollow f
			where f.user.id = :userId
			order by f.createdAt desc
			""")
	List<Object[]> findFollowingByUser(Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from BandFollow f where f.band.id = :bandId and f.user.id = :userId")
	void deleteByBandIdAndUserId(Long bandId, Long userId);

}
