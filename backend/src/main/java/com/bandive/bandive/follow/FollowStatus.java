package com.bandive.bandive.follow;

/** 팔로우 요청 상태. PENDING(승인 대기) → APPROVED(승인됨). 거절/언팔로우는 행 삭제. */
public enum FollowStatus {

	PENDING, APPROVED

}
