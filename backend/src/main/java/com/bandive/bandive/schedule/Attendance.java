package com.bandive.bandive.schedule;

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

import com.bandive.bandive.common.entity.BaseTimeEntity;
import com.bandive.bandive.guest.Guest;
import com.bandive.bandive.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 일정에 대한 참석. 대상은 실멤버({@code user}) 또는 게스트({@code guest}) 중 정확히 하나 (DB 부분 유니크 + CHECK
 * 제약이 배타성을 보장). 게스트는 관리자가 일정에 추가하며 항상 {@code ATTENDING} 이고, 그 일정에서 맡는 {@code session}(악기
 * 또는 "관객")을 함께 기록한다. 실멤버 행의 {@code session} 은 항상 null.
 */
@Getter
@Entity
@Table(name = "attendances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Attendance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guest_id")
	private Guest guest;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttendanceStatus status;

	/** 게스트 전용 — 그 일정에서 맡는 세션(악기 또는 "관객"). 실멤버는 null. */
	@Column(length = 30)
	private String session;

	public void changeStatus(AttendanceStatus status) {
		this.status = status;
	}

	public void changeSession(String session) {
		this.session = session;
	}

}
