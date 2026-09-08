package com.bandive.bandive.member.dto;

/** 밴드 리더 지정. {@code userId} 가 null 이면 리더 해제. */
public record LeaderRequest(Long userId) {
}
