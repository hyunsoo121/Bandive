package com.bandive.bandive.explore.dto;

import java.util.List;

import com.bandive.bandive.band.MyRelation;

/**
 * 탐색에서 한 밴드를 "구경"할 때의 상세. 탐색 화면 안에서만 쓰이며 밴드 컨텍스트로 진입하지 않는다. PUBLIC 밴드면 {@code videos} 에
 * 공개 합주 영상이 채워지고, FOLLOWERS 밴드면 비어 있다 (팔로우 후 밴드에서 열람).
 */
public record ExploreBandDetailResponse(ExploreBandResponse band, MyRelation myRelation,
		List<ExploreVideoResponse> videos) {
}
