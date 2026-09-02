package com.bandive.bandive.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

	boolean existsByBandIdAndUserId(Long bandId, Long userId);

	Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);

	List<BandMember> findAllByBandId(Long bandId);

	List<BandMember> findAllByUserId(Long userId);

	long countByBandId(Long bandId);

	/** 멤버 목록용 — user 를 fetch join 해서 N+1 을 피한다. */
	@Query("select bm from BandMember bm join fetch bm.user where bm.band.id = :bandId")
	List<BandMember> findAllByBandIdWithUser(Long bandId);

}
