package com.bandive.bandive.explore.service;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.band.BandVisibility;
import com.bandive.bandive.explore.ExploreRepository;
import com.bandive.bandive.media.Media;
import com.bandive.bandive.media.MediaLike;
import com.bandive.bandive.media.MediaLikeRepository;
import com.bandive.bandive.media.MediaPlatform;
import com.bandive.bandive.media.MediaType;
import com.bandive.bandive.media.MediaVisibility;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

class ExploreServiceTest extends RepositoryTest {

	@Autowired
	private ExploreRepository explore;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private com.bandive.bandive.follow.BandFollowRepository follows;

	@Autowired
	private MediaLikeRepository mediaLikes;

	@Autowired
	private TestEntityManager em;

	private ExploreService service;

	private User owner;

	@BeforeEach
	void setUp() {
		service = new ExploreService(explore, bands, bandMembers, follows, mediaLikes);
		owner = em.persist(Fixtures.user("owner"));
	}

	private Song track(Band band, String trackId, String title) {
		return em.persist(Song.builder()
			.band(band)
			.addedBy(owner)
			.title(title)
			.artist("아이유")
			.status(SongStatus.CONFIRMED)
			.sourceType(SongSourceType.SEARCH)
			.externalTrackId(trackId)
			.build());
	}

	private Media video(Band band, Song song, String url, MediaVisibility visibility) {
		return em.persist(Media.builder()
			.band(band)
			.song(song)
			.type(MediaType.REHEARSAL)
			.externalUrl(url)
			.platform(MediaPlatform.YOUTUBE)
			.visibility(visibility)
			.uploadedBy(owner)
			.build());
	}

	@Test
	void 탐색은_공개밴드의_공개영상만_모은다() {
		Band pub = em.persist(Fixtures.band("공개", BandVisibility.PUBLIC));
		Band priv = em.persist(Fixtures.band("비공개", BandVisibility.PRIVATE));
		em.persist(Fixtures.member(pub, owner, BandRole.OWNER));

		Song pubTrack = track(pub, "t1", "좋은 날");
		video(pub, pubTrack, "https://youtu.be/pub-public", MediaVisibility.LINK_PUBLIC);
		video(pub, pubTrack, "https://youtu.be/pub-members", MediaVisibility.MEMBERS_ONLY);
		video(priv, track(priv, "t1", "좋은 날 (비공개밴드)"), "https://youtu.be/priv", MediaVisibility.LINK_PUBLIC);

		Song manual = em.persist(Song.builder()
			.band(pub)
			.addedBy(owner)
			.title("직접입력곡")
			.status(SongStatus.CONFIRMED)
			.sourceType(SongSourceType.MANUAL)
			.build());
		video(pub, manual, "https://youtu.be/manual", MediaVisibility.LINK_PUBLIC);
		em.flush();
		em.clear();

		assertThat(service.tracks()).singleElement().satisfies(t -> {
			assertThat(t.externalTrackId()).isEqualTo("t1");
			assertThat(t.videoCount()).isEqualTo(1);
			assertThat(t.bandCount()).isEqualTo(1);
		});

		assertThat(service.trackVideos("t1", null, null)).singleElement()
			.satisfies(v -> assertThat(v.url()).isEqualTo("https://youtu.be/pub-public"),
					v -> assertThat(v.bandName()).isEqualTo("공개"), v -> assertThat(v.likeCount()).isZero());

		assertThat(service.bands()).extracting(b -> b.name()).containsExactly("공개");
	}

	@Test
	void 구글포토처럼_저장해둔_썸네일도_돌려준다() {
		Band pub = em.persist(Fixtures.band("공개2", BandVisibility.PUBLIC));
		em.persist(Fixtures.member(pub, owner, BandRole.OWNER));
		Song song = track(pub, "gphoto", "구글포토곡");
		em.persist(Media.builder()
			.band(pub)
			.song(song)
			.type(MediaType.REHEARSAL)
			.externalUrl("https://photos.app.goo.gl/abc")
			.platform(MediaPlatform.GOOGLE_PHOTOS)
			.thumbnailUrl("https://lh3.googleusercontent.com/pw/thumb.jpg")
			.visibility(MediaVisibility.LINK_PUBLIC)
			.uploadedBy(owner)
			.build());
		em.flush();
		em.clear();

		assertThat(service.trackVideos("gphoto", null, null)).singleElement()
			.satisfies(v -> assertThat(v.thumbnailUrl()).isEqualTo("https://lh3.googleusercontent.com/pw/thumb.jpg"));
	}

	@Test
	void 밴드_구경은_PUBLIC_이면_공개영상_FOLLOWERS_면_비어있고_PRIVATE_면_404() {
		Band pub = em.persist(Fixtures.band("공개", BandVisibility.PUBLIC));
		Band fol = em.persist(Fixtures.band("팔로워공개", BandVisibility.FOLLOWERS));
		Band priv = em.persist(Fixtures.band("비공개", BandVisibility.PRIVATE));
		em.persist(Fixtures.member(pub, owner, BandRole.OWNER));
		video(pub, track(pub, "tt", "good"), "https://youtu.be/a", MediaVisibility.LINK_PUBLIC);
		video(pub, track(pub, "tt", "good"), "https://youtu.be/b", MediaVisibility.MEMBERS_ONLY);
		em.flush();
		em.clear();

		var pubDetail = service.bandDetail(pub.getId(), null);
		assertThat(pubDetail.band().name()).isEqualTo("공개");
		assertThat(pubDetail.videos()).extracting(v -> v.url()).containsExactly("https://youtu.be/a");

		assertThat(service.bandDetail(fol.getId(), null).videos()).isEmpty();

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.bandDetail(priv.getId(), null))
			.isInstanceOf(com.bandive.bandive.common.exception.NotFoundException.class);
	}

	@Test
	void 영상_좋아요_상태가_반영된다() {
		Band pub = em.persist(Fixtures.band("공개", BandVisibility.PUBLIC));
		em.persist(Fixtures.member(pub, owner, BandRole.OWNER));
		User fan = em.persist(Fixtures.user("fan"));
		Media v = video(pub, track(pub, "t9", "밤편지"), "https://youtu.be/v9", MediaVisibility.LINK_PUBLIC);
		em.persist(MediaLike.builder().media(v).user(fan).createdAt(Instant.now()).build());
		em.flush();
		em.clear();

		assertThat(service.trackVideos("t9", null, fan.getId())).singleElement().satisfies(x -> {
			assertThat(x.likeCount()).isEqualTo(1);
			assertThat(x.likedByMe()).isTrue();
		});
		assertThat(service.trackVideos("t9", null, null).getFirst().likedByMe()).isFalse();
	}

	@Test
	void 다른_밴드_합주영상은_내_밴드를_제외한다() {
		Band mine = em.persist(Fixtures.band("우리밴드", BandVisibility.PUBLIC));
		Band other = em.persist(Fixtures.band("옆밴드", BandVisibility.PUBLIC));
		em.persist(Fixtures.member(mine, owner, BandRole.OWNER));
		video(mine, track(mine, "shared", "좋은 날"), "https://youtu.be/mine", MediaVisibility.LINK_PUBLIC);
		video(other, track(other, "shared", "좋은 날"), "https://youtu.be/other", MediaVisibility.LINK_PUBLIC);
		em.flush();
		em.clear();

		assertThat(service.trackVideos("shared", null, null)).extracting(v -> v.url())
			.containsExactlyInAnyOrder("https://youtu.be/mine", "https://youtu.be/other");
		assertThat(service.trackVideos("shared", mine.getId(), null)).singleElement()
			.satisfies(v -> assertThat(v.url()).isEqualTo("https://youtu.be/other"));
	}

}
