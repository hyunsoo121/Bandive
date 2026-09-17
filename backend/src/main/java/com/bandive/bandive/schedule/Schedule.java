package com.bandive.bandive.schedule;

import java.time.Instant;

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
import com.bandive.bandive.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Schedule extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScheduleType type;

	@Column(name = "date_time", nullable = false)
	private Instant dateTime;

	@Column(length = 200)
	private String location;

	/** 사용자가 붙인 제목. 선택 입력이라 null 가능 — 없으면 프론트가 종류 라벨로 대신 표시. */
	@Column(length = 100)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	/** 부분 수정 — null 인 필드는 그대로 둔다. */
	public void updateInfo(ScheduleType type, Instant dateTime, String location, String title) {
		if (type != null) {
			this.type = type;
		}
		if (dateTime != null) {
			this.dateTime = dateTime;
		}
		if (location != null) {
			this.location = location.isBlank() ? null : location;
		}
		if (title != null) {
			this.title = title.isBlank() ? null : title;
		}
	}

}
