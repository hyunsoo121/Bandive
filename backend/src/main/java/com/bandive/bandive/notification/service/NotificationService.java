package com.bandive.bandive.notification.service;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.notification.Notification;
import com.bandive.bandive.notification.NotificationRepository;
import com.bandive.bandive.notification.NotificationType;
import com.bandive.bandive.notification.dto.NotificationResponse;
import com.bandive.bandive.user.User;

/**
 * 알림 생성·조회. 새로고침/재진입 시에만 반영되는 방식(폴링·웹소켓 없음) — 나중에 실시간으로 바꿀 여지를 남겨 도메인 서비스(FollowService
 * 등)에서는 이 서비스만 호출하고 전달 방식은 신경 쓰지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

	private static final int RECENT_LIMIT = 50;

	private final NotificationRepository notifications;

	private final BandMemberRepository bandMembers;

	public NotificationService(NotificationRepository notifications, BandMemberRepository bandMembers) {
		this.notifications = notifications;
		this.bandMembers = bandMembers;
	}

	/** 밴드 멤버 전체에게 확인용 알림 (일정 등록·멤버 참가). {@code excludeUserId} 는 보통 그 이벤트를 일으킨 본인. */
	@Transactional
	public void notifyBandMembers(Band band, NotificationType type, User actor, Long excludeUserId) {
		List<BandMember> members = bandMembers.findAllByBandId(band.getId());
		List<Notification> toSave = members.stream()
			.map(BandMember::getUser)
			.filter(user -> !user.getId().equals(excludeUserId))
			.map(recipient -> Notification.builder().recipient(recipient).band(band).type(type).actor(actor).build())
			.toList();
		if (!toSave.isEmpty()) {
			notifications.saveAll(toSave);
		}
	}

	/** 특정 한 명에게 알림 (팔로우 신청 — 관리자에게). */
	@Transactional
	public void notifyUser(User recipient, Band band, NotificationType type, User actor) {
		notifications.save(Notification.builder().recipient(recipient).band(band).type(type).actor(actor).build());
	}

	/** 최근 알림 목록 (최신순, 최대 50개). */
	public List<NotificationResponse> list(Long userId) {
		return notifications.findRecentByRecipient(userId, Limit.of(RECENT_LIMIT))
			.stream()
			.map(NotificationResponse::from)
			.toList();
	}

	public long unreadCount(Long userId) {
		return notifications.countByRecipientIdAndReadAtIsNull(userId);
	}

	/** 본인 알림만 읽음 처리 가능. */
	@Transactional
	public void markRead(Long id, Long userId) {
		Notification n = notifications.findById(id)
			.orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."));
		if (!n.getRecipient().getId().equals(userId)) {
			throw new NotFoundException("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.");
		}
		n.markRead();
	}

	@Transactional
	public void markAllRead(Long userId) {
		notifications.findAllByRecipientIdAndReadAtIsNull(userId).forEach(Notification::markRead);
	}

}
