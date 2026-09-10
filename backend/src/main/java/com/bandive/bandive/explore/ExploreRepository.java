package com.bandive.bandive.explore;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaVisibility;

/**
 * 탐색 전용 조회. 곡(트랙) 간 조인 키는 {@code song.externalTrackId} (검색으로 추가한 곡만). 노출 조건 = 밴드가 PUBLIC
 * 이고 영상이 LINK_PUBLIC (= 실효 전체공개).
 */
public interface ExploreRepository extends Repository<Media, Long> {

	/** [externalTrackId, title, artist, artworkUrl, bandCount, videoCount] — 영상 많은 순. */
	@Query("""
			select s.externalTrackId, max(s.title), max(s.artist), max(s.artworkUrl),
			       count(distinct m.band.id), count(m.id)
			from Media m join m.song s
			where m.visibility = :mediaPublic
			  and m.band.visibility = :bandPublic
			  and s.externalTrackId is not null
			group by s.externalTrackId
			order by count(m.id) desc, max(s.title) asc
			""")
	List<Object[]> tracks(MediaVisibility mediaPublic, BandVisibility bandPublic);

	@Query("""
			select m from Media m
			  join fetch m.band
			  join fetch m.song
			  join fetch m.uploadedBy
			where m.song.externalTrackId = :trackId
			  and m.visibility = :mediaPublic
			  and m.band.visibility = :bandPublic
			order by m.createdAt desc
			""")
	List<Media> videosByTrack(String trackId, MediaVisibility mediaPublic, BandVisibility bandPublic);

	/** 같은 트랙의 다른 밴드 공개 합주 영상 (내 밴드 제외). */
	@Query("""
			select m from Media m
			  join fetch m.band
			  join fetch m.song
			  join fetch m.uploadedBy
			where m.song.externalTrackId = :trackId
			  and m.band.id <> :excludeBandId
			  and m.visibility = :mediaPublic
			  and m.band.visibility = :bandPublic
			order by m.createdAt desc
			""")
	List<Media> videosByTrackExcludingBand(String trackId, Long excludeBandId, MediaVisibility mediaPublic,
			BandVisibility bandPublic);

	@Query("""
			select m from Media m
			  join fetch m.band
			  left join fetch m.song
			  join fetch m.uploadedBy
			where m.band.id = :bandId
			  and m.visibility = :mediaPublic
			  and m.band.visibility = :bandPublic
			order by m.createdAt desc
			""")
	List<Media> videosByBand(Long bandId, MediaVisibility mediaPublic, BandVisibility bandPublic);

}
