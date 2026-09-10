package com.bandive.bandive.explore.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.explore.dto.ExploreBandDetailResponse;
import com.bandive.bandive.explore.dto.ExploreBandResponse;
import com.bandive.bandive.explore.dto.ExploreTrackResponse;
import com.bandive.bandive.explore.dto.ExploreVideoResponse;
import com.bandive.bandive.explore.service.ExploreService;

/** 탐색 — 전부 공개 GET (비로그인 열람 가능). */
@RestController
public class ExploreController {

	private final ExploreService exploreService;

	public ExploreController(ExploreService exploreService) {
		this.exploreService = exploreService;
	}

	@GetMapping("/api/explore/bands")
	public List<ExploreBandResponse> bands() {
		return exploreService.bands();
	}

	/** 탐색 안에서 밴드 한 곳 구경 (밴드 컨텍스트로 진입하지 않음). */
	@GetMapping("/api/explore/bands/{bandId}")
	public ExploreBandDetailResponse bandDetail(@PathVariable Long bandId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return exploreService.bandDetail(bandId, principal != null ? principal.getId() : null);
	}

	@GetMapping("/api/explore/songs")
	public List<ExploreTrackResponse> songs() {
		return exploreService.tracks();
	}

	/** {@code excludeBandId} 를 주면 그 밴드 영상은 제외 — 곡 상세의 "다른 밴드 합주 영상". */
	@GetMapping("/api/explore/songs/{externalTrackId}/media")
	public List<ExploreVideoResponse> songVideos(@PathVariable String externalTrackId,
			@RequestParam(name = "excludeBandId", required = false) Long excludeBandId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return exploreService.trackVideos(externalTrackId, excludeBandId, principal != null ? principal.getId() : null);
	}

}
