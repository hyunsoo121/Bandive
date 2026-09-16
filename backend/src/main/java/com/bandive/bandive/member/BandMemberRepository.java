package com.bandive.bandive.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

	boolean existsByBandIdAndUserId(Long bandId, Long userId);

	Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);

	/** 밴드당 관리자(OWNER) 는 항상 정확히 한 명 — 알림 수신자 조회 등에 씀. */
	Optional<BandMember> findByBandIdAndRole(Long bandId, BandRole role);

	List<BandMember> findAllByBandId(Long bandId);

	List<BandMember> findAllByUserId(Long userId);

	long countByBandId(Long bandId);

	/** 멤버 목록용 — user 를 fetch join 해서 N+1 을 피한다. */
	@Query("select bm from BandMember bm join fetch bm.user where bm.band.id = :bandId")
	List<BandMember> findAllByBandIdWithUser(Long bandId);

	/** 유저 프로필용 — 그 유저가 속한 밴드들. band 를 fetch join. */
	@Query("select bm from BandMember bm join fetch bm.band where bm.user.id = :userId")
	List<BandMember> findAllByUserIdWithBand(Long userId);

	/** 밴드의 리더 플래그를 모두 해제 (리더 재지정 전에 호출). */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update BandMember bm set bm.leader = false where bm.band.id = :bandId and bm.leader = true")
	void clearLeader(Long bandId);

}
