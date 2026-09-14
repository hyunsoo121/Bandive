package com.bandive.bandive.band;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BandRepository extends JpaRepository<Band, Long> {

	/** 탐색 목록용 — 지정한 공개범위의 밴드들, 최근 생성 순. */
	List<Band> findByVisibilityInOrderByCreatedAtDesc(Collection<BandVisibility> visibilities);

}
