import { api } from './client';
import type { NotificationDto } from './types';

/** 최근 알림 목록 (최대 50개, 최신순). 로그인 필요. */
export const listNotifications = () => api.get<NotificationDto[]>('/api/notifications');

/** 안 읽은 알림 개수만. 목록을 이미 들고 있으면 그걸로 계산하는 게 더 간단해서 지금은 안 쓰지만,
 * 나중에 실시간(웹소켓) 붙이기 전 가벼운 폴링용으로 남겨둔다. */
export const unreadCount = () => api.get<{ count: number }>('/api/notifications/unread-count');

export const markNotificationRead = (id: string) => api.post<void>(`/api/notifications/${id}/read`);

export const markAllNotificationsRead = () => api.post<void>('/api/notifications/read-all');
