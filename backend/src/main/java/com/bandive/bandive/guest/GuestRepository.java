package com.bandive.bandive.guest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

	List<Guest> findAllByBandIdOrderByNameAsc(Long bandId);

	Optional<Guest> findByIdAndBandId(Long id, Long bandId);

	boolean existsByIdAndBandId(Long id, Long bandId);

	boolean existsByBandIdAndName(Long bandId, String name);

}
