package com.bandive.bandive.song.dto;

/**
 * 파트 배정. {@code userId} 는 실멤버, {@code guestId} 는 게스트 — 최대 하나만 지정한다. 둘 다 null 이면 배정 해제.
 */
public record PartAssignRequest(Long userId, Long guestId) {
}
