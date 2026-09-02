package com.bandive.bandive.invite;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

	Optional<InviteCode> findByCode(String code);

	boolean existsByCode(String code);

	Optional<InviteCode> findByBandId(Long bandId);

	void deleteByBandId(Long bandId);

}
