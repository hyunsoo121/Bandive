package com.bandive.bandive.explore.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.band.MyRelation;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.explore.ExploreRepository;
import com.bandive.bandive.explore.dto.ExploreBandDetailResponse;
import com.bandive.bandive.explore.dto.ExploreBandResponse;
import com.bandive.bandive.explore.dto.ExploreTrackResponse;
import com.bandive.bandive.explore.dto.ExploreVideoResponse;
import com.bandive.bandive.follow.BandFollowRepository;
import com.bandive.bandive.follow.FollowStatus;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaLikeRepository;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.member.BandMemberRepository;

/**
 * 탐색 — 로그인 없이도 열람 가능(좋아요만 로그인 필요). 노출 대상은 PUBLIC 밴드의 LINK_PUBLIC 영상, 곡은 검색으로 추가돼
 * {@code externalTrackId} 가 있는 것만.
 */
@Service
@Transactional(readOnly = true)
public class ExploreService {

	private static final List<BandVisibility> DISCOVERABLE = List.of(BandVisibility.PUBLIC, BandVisibility.FOLLOWERS);

	private final ExploreRepository explore;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final BandFollowRepository follows;

	private final MediaLikeRepository mediaLikes;

	public ExploreService(ExploreRepository explore, BandRepository bands, BandMemberRepository bandMembers,
			BandFollowRepository follows, MediaLikeRepository mediaLikes) {
		this.explore = explore;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.follows = follows;
		this.mediaLikes = mediaLikes;
	}

	public List<ExploreBandResponse> bands() {
		return bands.findByVisibilityInOrderByCreatedAtDesc(DISCOVERABLE)
			.stream()
			.map(band -> ExploreBandResponse.from(band, bandMembers.countByBandId(band.getId())))
			.toList();
	}

	public ExploreBandDetailResponse bandDetail(Long bandId, Long viewerUserId) {
		Band band = bands.findById(bandId)
			.filter(b -> DISCOVERABLE.contains(b.getVisibility()))
			.orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));

		ExploreBandResponse card = ExploreBandResponse.from(band, bandMembers.countByBandId(bandId));
		List<ExploreVideoResponse> videos = band.getVisibility() == BandVisibility.PUBLIC
				? withLikes(explore.videosByBand(bandId, MediaVisibility.LINK_PUBLIC, BandVisibility.PUBLIC),
						viewerUserId)
				: List.of();
		return new ExploreBandDetailResponse(card, relationOf(bandId, viewerUserId), videos);
	}

	public List<ExploreTrackResponse> tracks() {
		return explore.tracks(MediaVisibility.LINK_PUBLIC, BandVisibility.PUBLIC)
			.stream()
			.map(ExploreTrackResponse::of)
			.toList();
	}

	public List<ExploreVideoResponse> trackVideos(String externalTrackId, Long viewerUserId) {
		return withLikes(explore.videosByTrack(externalTrackId, MediaVisibility.LINK_PUBLIC, BandVisibility.PUBLIC),
				viewerUserId);
	}

	private List<ExploreVideoResponse> withLikes(List<Media> videos, Long viewerUserId) {
		if (videos.isEmpty()) {
			return List.of();
		}
		List<Long> ids = videos.stream().map(Media::getId).toList();
		Map<Long, Long> likeCounts = mediaLikes.countByMediaIds(ids)
			.stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
		Set<Long> likedByMe = viewerUserId == null ? Set.of()
				: new HashSet<>(mediaLikes.findLikedMediaIds(viewerUserId, ids));
		return videos.stream()
			.map(media -> ExploreVideoResponse.from(media, likeCounts.getOrDefault(media.getId(), 0L),
					likedByMe.contains(media.getId())))
			.toList();
	}

	private MyRelation relationOf(Long bandId, Long userId) {
		if (userId == null) {
			return MyRelation.NONE;
		}
		if (bandMembers.existsByBandIdAndUserId(bandId, userId)) {
			return MyRelation.MEMBER;
		}
		return follows.findByBandIdAndUserId(bandId, userId)
			.map(f -> f.getStatus() == FollowStatus.APPROVED ? MyRelation.FOLLOWER : MyRelation.PENDING)
			.orElse(MyRelation.NONE);
	}

}
