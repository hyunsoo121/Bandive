package com.bandive.bandive.song;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, Long> {

	/** 폴더 삭제 시 소속 곡을 미분류로. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Song s set s.folder = null where s.folder.id = :folderId")
	void clearFolder(Long folderId);

	List<Song> findAllByBandId(Long bandId);

	List<Song> findAllByBandIdAndStatus(Long bandId, SongStatus status);

	/** 미분류(folder=null) 그룹의 곡 수 — 새 곡의 position 계산용. */
	int countByBandIdAndStatusAndFolderIsNull(Long bandId, SongStatus status);

	/** 특정 폴더의 곡 수 — 폴더로 옮길 때 맨 끝 position 계산용. */
	int countByFolderId(Long folderId);

	/** 한 폴더 안 곡들 (정렬 재지정용). */
	List<Song> findByBandIdAndStatusAndFolderIdOrderByPositionAsc(Long bandId, SongStatus status, Long folderId);

	/** 미분류 그룹 곡들 (정렬 재지정용). */
	List<Song> findByBandIdAndStatusAndFolderIsNullOrderByPositionAsc(Long bandId, SongStatus status);

	/** 목록 응답용 — addedBy·parts·배정멤버를 한 번에 fetch. status 가 null 이면 전체. */
	@Query("""
			select distinct s from Song s
			  join fetch s.addedBy
			  left join fetch s.folder
			  left join fetch s.parts p
			  left join fetch p.assignedMember m
			  left join fetch m.user
			  left join fetch p.assignedGuest
			where s.band.id = :bandId
			  and (:status is null or s.status = :status)
			""")
	List<Song> findAllForBand(Long bandId, SongStatus status);

	@Query("""
			select s from Song s
			  join fetch s.addedBy
			  left join fetch s.parts p
			  left join fetch p.assignedMember m
			  left join fetch m.user
			  left join fetch p.assignedGuest
			where s.id = :id
			""")
	Optional<Song> findByIdWithDetails(Long id);

}
