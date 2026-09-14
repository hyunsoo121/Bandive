package com.bandive.bandive.song.dto;

/**
 * 투표/취소 후 상태. 프론트가 버튼만 갱신하면 되게.
 */
public record VoteResult(long voteCount, boolean votedByMe) {
}
