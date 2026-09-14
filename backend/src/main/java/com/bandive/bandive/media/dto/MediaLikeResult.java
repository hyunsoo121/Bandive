package com.bandive.bandive.media.dto;

/**
 * 좋아요/취소 후 상태. 프론트가 버튼만 갱신하면 되게.
 */
public record MediaLikeResult(long likeCount, boolean likedByMe) {
}
