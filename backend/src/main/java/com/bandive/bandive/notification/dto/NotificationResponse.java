package com.bandive.bandive.notification.dto;

import java.time.Instant;

import com.bandive.bandive.notification.Notification;
import com.bandive.bandive.notification.NotificationType;

/**
 * 알림 응답. {@code actorId}/{@code actorNickname} 은 이 알림을 유발한 사람 — {@code FOLLOW_REQUESTED}
 * 는 승인/거절 액션의 대상(팔로우 신청자)이 그대로 이 사람이라 별도 필드 없이 재사용한다.
 */
public record NotificationResponse(Long id, NotificationType type, Long bandId, String bandName, Long actorId,
		String actorNickname, Instant readAt, Instant createdAt) {

	public static NotificationResponse from(Notification n) {
		return new NotificationResponse(n.getId(), n.getType(), n.getBand().getId(), n.getBand().getName(),
				n.getActor() != null ? n.getActor().getId() : null,
				n.getActor() != null ? n.getActor().getNickname() : null, n.getReadAt(), n.getCreatedAt());
	}

}
