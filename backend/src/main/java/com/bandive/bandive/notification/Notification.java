package com.bandive.bandive.notification;

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

/**
 * 알림 한 건. {@code actor} 는 이 알림을 유발한 사람 (예: 팔로우를 신청한 사람). 시스템성 알림은 없어서 항상 채워지지만, 필드 자체는
 * nullable — 나중에 시스템 알림이 생길 걸 대비.
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "band_id", nullable = false)
	private Band band;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Column(name = "read_at")
	private Instant readAt;

	public void markRead() {
		if (this.readAt == null) {
			this.readAt = Instant.now();
		}
	}

	public boolean isUnread() {
		return this.readAt == null;
	}

}
