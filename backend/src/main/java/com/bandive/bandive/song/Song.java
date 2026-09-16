package com.bandive.bandive.song;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.entity.BaseTimeEntity;
import com.bandive.bandive.song.folder.SongFolder;
import com.bandive.bandive.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "songs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Song extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 200)
	private String artist;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SongStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private SongSourceType sourceType;

	/** 외부 음원 API 트랙 식별자 (source_type = SEARCH 일 때) */
	@Column(name = "external_track_id", length = 100)
	private String externalTrackId;

	@Column(columnDefinition = "text")
	private String memo;

	@Column(name = "reference_video_url", length = 500)
	private String referenceVideoUrl;

	/** 앨범 커버 이미지 URL (SEARCH 로 추가 시 외부 음원 API 에서). 직접 입력 곡은 null. */
	@Column(name = "artwork_url", length = 500)
	private String artworkUrl;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "added_by", nullable = false)
	private User addedBy;

	/** 속한 폴더. null = 미분류. 폴더는 곡의 status 와 같은 status 여야 한다. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "folder_id")
	private SongFolder folder;

	/** (band, status, folder 또는 미분류) 그룹 안에서의 수동 정렬 위치. 0 부터. */
	@Builder.Default
	@Column(nullable = false)
	private int position = 0;

	@Builder.Default
	@OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SongPart> parts = new ArrayList<>();

	public void addPart(SongPart part) {
		this.parts.add(part);
		part.assignToSong(this);
	}

	/** 세션 구성 수정 시 줄어든 슬롯 제거. orphanRemoval 이라 리스트에서 빠지면 삭제된다. */
	public void removeParts(Collection<SongPart> toRemove) {
		this.parts.removeAll(toRemove);
	}

	public boolean isConfirmed() {
		return this.status == SongStatus.CONFIRMED;
	}

	/** WISHLIST → CONFIRMED 승격. 폴더는 status 별이라 승격 시 미분류로 뺀다. 멱등. */
	public void confirm() {
		this.status = SongStatus.CONFIRMED;
		this.folder = null;
	}

	/** 폴더로 이동. null 이면 미분류. */
	public void moveToFolder(SongFolder folder) {
		this.folder = folder;
	}

	/** 부분 수정 — 제목/아티스트/메모/참고영상. 위시리스트·합주곡 상태 모두 가능. null 유지 여부는 서비스에서 결정해 넘긴다. */
	public void updateDetails(String title, String artist, String memo, String referenceVideoUrl) {
		this.title = title;
		this.artist = artist;
		this.memo = memo;
		this.referenceVideoUrl = referenceVideoUrl;
	}

	/** 그룹 안 정렬 위치 지정. */
	public void moveToPosition(int position) {
		this.position = position;
	}

}
