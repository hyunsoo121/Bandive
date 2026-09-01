package com.bandive.bandive.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

	boolean existsByBandIdAndUserId(Long bandId, Long userId);

	Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);

	List<BandMember> findAllByBandId(Long bandId);

	List<BandMember> findAllByUserId(Long userId);

}
