package com.bandive.bandive.song.dto;

/**
 * 파트 배정. {@code userId} 가 null 이면 배정 해제.
 */
public record PartAssignRequest(Long userId) {
}
