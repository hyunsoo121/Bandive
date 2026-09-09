package com.bandive.bandive.band;

/**
 * 밴드 공개범위.
 *
 * <ul>
 * <li>{@code PRIVATE} — 멤버만. 비멤버에겐 밴드 존재 자체를 숨긴다(404).
 * <li>{@code FOLLOWERS} — 멤버 + 관리자가 승인한 팔로워. 비팔로워는 밴드 표지(이름·소개)만 볼 수 있고 콘텐츠는 403.
 * <li>{@code PUBLIC} — 누구나 열람. 탐색에 노출된다.
 * </ul>
 *
 * 영상 개별 공개범위와의 관계: 영상 실효 공개범위 = min(밴드, 영상).
 */
public enum BandVisibility {

	PRIVATE, FOLLOWERS, PUBLIC

}
