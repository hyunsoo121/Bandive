package com.bandive.bandive.notification;

/**
 * 알림 종류. {@code FOLLOW_REQUESTED} 만 액션(승인/거절)이 필요하고 나머지는 확인용.
 *
 * <ul>
 * <li>{@code SCHEDULE_CREATED} — 밴드에 새 일정이 등록됨 (등록자 제외 멤버 전체)
 * <li>{@code MEMBER_JOINED} — 초대 코드로 새 멤버가 들어옴 (신규 멤버 제외 기존 멤버 전체)
 * <li>{@code FOLLOW_REQUESTED} — FOLLOWERS 밴드에 팔로우 요청이 와서 승인 대기 중 (관리자, 액션 필요)
 * <li>{@code FOLLOW_AUTO_APPROVED} — PUBLIC 밴드라 팔로우가 즉시 승인됨 (관리자, 확인용)
 * </ul>
 */
public enum NotificationType {

	SCHEDULE_CREATED, MEMBER_JOINED, FOLLOW_REQUESTED, FOLLOW_AUTO_APPROVED

}
