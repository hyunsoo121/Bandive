package com.bandive.bandive.media;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaRepository extends JpaRepository<Media, Long> {

	List<Media> findAllByBandId(Long bandId);

	List<Media> findAllByBandIdAndScheduleId(Long bandId, Long scheduleId);

	/**
	 * 공개범위 필터가 적용된 목록. {@code includeMembersOnly=false} 면 LINK_PUBLIC 만.
	 * {@code scheduleId=null} 이면 일정 필터 없음.
	 */
	@Query("""
			select m from Media m
			  join fetch m.uploadedBy
			  left join fetch m.schedule
			where m.band.id = :bandId
			  and (:scheduleId is null or m.schedule.id = :scheduleId)
			  and (:includeMembersOnly = true or m.visibility = com.bandive.bandive.media.MediaVisibility.LINK_PUBLIC)
			order by m.createdAt desc
			""")
	List<Media> findVisible(Long bandId, Long scheduleId, boolean includeMembersOnly);

	@Query("""
			select m from Media m
			  join fetch m.uploadedBy
			where m.schedule.id in :scheduleIds
			  and (:includeMembersOnly = true or m.visibility = com.bandive.bandive.media.MediaVisibility.LINK_PUBLIC)
			""")
	List<Media> findVisibleByScheduleIds(Collection<Long> scheduleIds, boolean includeMembersOnly);

}
