package com.bandive.bandive.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.notification.dto.NotificationResponse;
import com.bandive.bandive.notification.service.NotificationService;

@RestController
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	/** 최근 알림 목록 (로그인 필요). */
	@GetMapping("/api/notifications")
	public List<NotificationResponse> list(@CurrentUser Long userId) {
		return notificationService.list(userId);
	}

	@GetMapping("/api/notifications/unread-count")
	public Map<String, Long> unreadCount(@CurrentUser Long userId) {
		return Map.of("count", notificationService.unreadCount(userId));
	}

	@PostMapping("/api/notifications/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markRead(@PathVariable Long id, @CurrentUser Long userId) {
		notificationService.markRead(id, userId);
	}

	@PostMapping("/api/notifications/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markAllRead(@CurrentUser Long userId) {
		notificationService.markAllRead(userId);
	}

}
