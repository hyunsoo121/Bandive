package com.bandive.bandive.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.bandive.bandive.common.entity.BaseTimeEntity;
import com.bandive.bandive.member.BandMember;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 곡의 세션 슬롯. 곡 등록 시점에 생성되며, assignedMember 는 곡이 CONFIRMED 일 때만 채워진다. instrument 는 자유 문자열
 * (GUITAR 등 표준값 권장이나 "색소폰" 같은 값도 허용).
 */
@Getter
@Entity
@Table(name = "song_parts",
		uniqueConstraints = @UniqueConstraint(name = "uq_song_part_slot",
				columnNames = { "song_id", "instrument", "part_index" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SongPart extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "song_id", nullable = false)
	private Song song;

	@Column(nullable = false, length = 20)
	private String instrument;

	/** 동일 악기 내 순번 (1부터) */
	@Column(name = "part_index", nullable = false)
	private int partIndex;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_member_id")
	private BandMember assignedMember;

	void assignToSong(Song song) {
		this.song = song;
	}

	/** CONFIRMED 곡에서만 호출되어야 한다 (SongService 가 강제). */
	public void assignTo(BandMember member) {
		this.assignedMember = member;
	}

	public void unassign() {
		this.assignedMember = null;
	}

}
