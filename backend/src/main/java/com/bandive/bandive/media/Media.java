package com.bandive.bandive.media;

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
import jakarta.persistence.Table;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.entity.BaseTimeEntity;
import com.bandive.bandive.schedule.Schedule;
import com.bandive.bandive.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 URL 영상 메타데이터. 파일 자체는 저장하지 않는다.
 */
@Getter
@Entity
@Table(name = "media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Media extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	/** 특정 일정과의 선택적 연결 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id")
	private Schedule schedule;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaType type;

	@Column(name = "external_url", nullable = false, length = 500)
	private String externalUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaPlatform platform;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaVisibility visibility;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by", nullable = false)
	private User uploadedBy;

	public void changeVisibility(MediaVisibility visibility) {
		this.visibility = visibility;
	}

}
