package com.bandive.bandive.band;

/**
 * "현재 사용자가 이 밴드와 맺고 있는 관계" — 응답에 담겨 프론트가 팔로우 버튼/콘텐츠 표시를 결정한다.
 *
 * <ul>
 * <li>{@code MEMBER} — 초대코드로 가입한 밴드원
 * <li>{@code FOLLOWER} — 팔로우 요청이 승인됨. FOLLOWERS 밴드는 이걸로 콘텐츠 열람이 풀리고, PUBLIC 밴드는 콘텐츠가 이미
 * 전체공개라 순수 구독/북마크 의미(요청 즉시 자동 승인)
 * <li>{@code PENDING} — 팔로우 요청했으나 관리자 승인 대기 중 (FOLLOWERS 밴드만 해당 — PUBLIC 은 즉시 승인)
 * <li>{@code NONE} — 아무 관계 없음 (비로그인 포함)
 * </ul>
 */
public enum MyRelation {

	MEMBER, FOLLOWER, PENDING, NONE

}
