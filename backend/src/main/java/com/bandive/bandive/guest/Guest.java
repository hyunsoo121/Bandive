package com.bandive.bandive.guest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게스트 멤버 — 계정 없이 이름만 있는 밴드 구성원. 곡 세션 배정({@code SongPart})과 일정 출결({@code Attendance})에서만
 * 참조된다. 로그인·권한·파트·리더와는 무관하며 등록/수정/삭제는 관리자만 한다.
 */
@Getter
@Entity
@Table(name = "band_guests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Guest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	@Column(nullable = false, length = 50)
	private String name;

	public void rename(String name) {
		this.name = name;
	}

}
