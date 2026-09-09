package com.bandive.bandive.band;

/**
 * "현재 사용자가 이 밴드와 맺고 있는 관계" — 응답에 담겨 프론트가 팔로우 버튼/콘텐츠 표시를 결정한다.
 *
 * <ul>
 * <li>{@code MEMBER} — 초대코드로 가입한 밴드원
 * <li>{@code FOLLOWER} — 팔로우 요청이 승인됨 (콘텐츠 열람 가능, 편집 불가)
 * <li>{@code PENDING} — 팔로우 요청했으나 관리자 승인 대기 중
 * <li>{@code NONE} — 아무 관계 없음 (비로그인 포함)
 * </ul>
 */
public enum MyRelation {

	MEMBER, FOLLOWER, PENDING, NONE

}
