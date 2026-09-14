package com.bandive.bandive.song.folder.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongRepository;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.folder.SongFolder;
import com.bandive.bandive.song.folder.SongFolderRepository;
import com.bandive.bandive.song.folder.dto.CreateFolderRequest;
import com.bandive.bandive.song.folder.dto.FolderOrderRequest;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongFolderServiceTest extends RepositoryTest {

	@Autowired
	private SongFolderRepository folders;

	@Autowired
	private SongRepository songs;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private com.bandive.bandive.follow.BandFollowRepository follows;

	@Autowired
	private TestEntityManager em;

	private SongFolderService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new SongFolderService(folders, songs, bands, bandMembers,
				new com.bandive.bandive.common.security.BandAccessGuard(bands, bandMembers, follows));
		band = em.persist(Fixtures.band("A"));
		ownerId = join("owner", BandRole.OWNER);
		memberId = join("member", BandRole.MEMBER);
	}

	private Long join(String kakaoId, BandRole role) {
		User user = em.persist(Fixtures.user(kakaoId));
		em.persist(Fixtures.member(band, user, role));
		return user.getId();
	}

	private CreateFolderRequest req(String name, SongStatus status) {
		return new CreateFolderRequest(name, status);
	}

	@Test
	void 생성은_관리자만_이고_목록_끝에_붙는다() {
		assertThatThrownBy(() -> service.create(band.getId(), memberId, req("커버곡", SongStatus.WISHLIST)))
			.isInstanceOf(ForbiddenException.class);

		assertThat(service.create(band.getId(), ownerId, req("커버곡", SongStatus.WISHLIST)).position()).isZero();
		assertThat(service.create(band.getId(), ownerId, req("자작곡", SongStatus.WISHLIST)).position()).isEqualTo(1);
		// status 가 다르면 별도 순번
		assertThat(service.create(band.getId(), ownerId, req("정규", SongStatus.CONFIRMED)).position()).isZero();
	}

	@Test
	void 삭제하면_소속_곡은_미분류로() {
		Long folderId = service.create(band.getId(), ownerId, req("커버곡", SongStatus.WISHLIST)).id();
		SongFolder folder = folders.findById(folderId).orElseThrow();
		User owner = em.find(User.class, ownerId);
		Song song = em.persist(Song.builder()
			.band(band)
			.addedBy(owner)
			.title("t")
			.status(SongStatus.WISHLIST)
			.sourceType(SongSourceType.MANUAL)
			.folder(folder)
			.build());
		em.flush();

		service.delete(folderId, ownerId);
		em.flush();
		em.clear();

		assertThat(folders.findById(folderId)).isEmpty();
		assertThat(songs.findById(song.getId()).orElseThrow().getFolder()).isNull();
	}

	@Test
	void 순서_재지정_목록이_불완전하면_400() {
		Long a = service.create(band.getId(), ownerId, req("A", SongStatus.WISHLIST)).id();
		Long b = service.create(band.getId(), ownerId, req("B", SongStatus.WISHLIST)).id();
		Long c = service.create(band.getId(), ownerId, req("C", SongStatus.WISHLIST)).id();
		em.flush();

		var reordered = service.reorder(band.getId(), ownerId,
				new FolderOrderRequest(SongStatus.WISHLIST, List.of(c, a, b)));
		assertThat(reordered).extracting(f -> f.name()).containsExactly("C", "A", "B");

		assertThatThrownBy(() -> service.reorder(band.getId(), ownerId,
				new FolderOrderRequest(SongStatus.WISHLIST, List.of(a, b))))
			.isInstanceOf(ValidationException.class);
	}

}
