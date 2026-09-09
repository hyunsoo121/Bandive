package com.bandive.bandive.song.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.guest.Guest;
import com.bandive.bandive.guest.GuestRepository;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.song.SongPartRepository;
import com.bandive.bandive.song.SongRepository;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.VoteRepository;
import com.bandive.bandive.song.dto.SongCreateRequest;
import com.bandive.bandive.song.dto.SongCreateRequest.SessionSlot;
import com.bandive.bandive.song.dto.SongPartResponse;
import com.bandive.bandive.song.dto.SongResponse;
import com.bandive.bandive.song.dto.VoteResult;
import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongServiceTest extends RepositoryTest {

	@Autowired
	private SongRepository songs;

	@Autowired
	private SongPartRepository parts;

	@Autowired
	private VoteRepository votes;

	@Autowired
	private BandRepository bands;

	@Autowired
	private BandMemberRepository bandMembers;

	@Autowired
	private GuestRepository guests;

	@Autowired
	private UserRepository users;

	@Autowired
	private com.bandive.bandive.song.folder.SongFolderRepository folders;

	@Autowired
	private TestEntityManager em;

	private SongService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new SongService(songs, parts, votes, bands, bandMembers, guests, users, folders,
				new StubMusicSearchService(),
				new com.bandive.bandive.common.security.BandAccessGuard(bands, bandMembers));
		band = em.persist(Fixtures.band("A"));
		ownerId = joinMember("owner", BandRole.OWNER);
		memberId = joinMember("member", BandRole.MEMBER);
	}

	private Long joinMember(String kakaoId, BandRole role) {
		User user = em.persist(Fixtures.user(kakaoId));
		em.persist(Fixtures.member(band, user, role));
		return user.getId();
	}

	private SongCreateRequest manual(List<SessionSlot> sessions) {
		return new SongCreateRequest("곡", "아티스트", SongSourceType.MANUAL, null, null, "메모", null, sessions);
	}

	// ── add ──────────────────────────────────────────────

	@Test
	void 세션_구성만큼_파트_슬롯이_생성된다() {
		SongResponse created = service.add(band.getId(), memberId,
				manual(List.of(new SessionSlot("GUITAR", 2), new SessionSlot("DRUM", 1))));
		em.flush();

		assertThat(created.status()).isEqualTo(SongStatus.WISHLIST);
		assertThat(created.parts()).extracting(SongPartResponse::instrument, SongPartResponse::partIndex)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("DRUM", 1),
					org.assertj.core.groups.Tuple.tuple("GUITAR", 1), org.assertj.core.groups.Tuple.tuple("GUITAR", 2));
	}

	@Test
	void 비멤버는_곡을_추가할_수_없다() {
		Long outsiderId = em.persist(Fixtures.user("out")).getId();

		assertThatThrownBy(() -> service.add(band.getId(), outsiderId, manual(null)))
			.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void SEARCH_인데_트랙id가_없으면_400() {
		SongCreateRequest req = new SongCreateRequest("곡", "a", SongSourceType.SEARCH, "  ", null, null, null, null);

		assertThatThrownBy(() -> service.add(band.getId(), memberId, req)).isInstanceOf(ValidationException.class)
			.satisfies(ex -> assertThat(((ValidationException) ex).getCode()).isEqualTo("EXTERNAL_TRACK_ID_REQUIRED"));
	}

	// ── list / vote ──────────────────────────────────────

	@Test
	void 목록은_득표수와_votedByMe_를_담는다() {
		Long a = service.add(band.getId(), ownerId, manual(null)).id();
		service.add(band.getId(), ownerId, manual(null));
		em.flush();
		service.vote(a, memberId);
		em.flush();
		em.clear();

		List<SongResponse> list = service.list(band.getId(), null, memberId);

		assertThat(list).hasSize(2);
		SongResponse voted = list.stream().filter(s -> s.id().equals(a)).findFirst().orElseThrow();
		assertThat(voted.voteCount()).isEqualTo(1);
		assertThat(voted.votedByMe()).isTrue();
		assertThat(list.stream().filter(s -> !s.id().equals(a)).findFirst().orElseThrow().votedByMe()).isFalse();
	}

	@Test
	void 없는_밴드_목록은_404() {
		assertThatThrownBy(() -> service.list(999L, null, null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void 투표와_취소는_멱등이다() {
		Long songId = service.add(band.getId(), ownerId, manual(null)).id();
		em.flush();

		service.vote(songId, memberId);
		VoteResult twice = service.vote(songId, memberId);
		assertThat(twice.voteCount()).isEqualTo(1);
		assertThat(twice.votedByMe()).isTrue();

		service.unvote(songId, memberId);
		VoteResult goneAgain = service.unvote(songId, memberId);
		assertThat(goneAgain.voteCount()).isZero();
		assertThat(goneAgain.votedByMe()).isFalse();
	}

	// ── confirm ──────────────────────────────────────────

	@Test
	void 관리자는_곡을_확정한다() {
		Long songId = service.add(band.getId(), memberId, manual(null)).id();
		em.flush();

		SongResponse confirmed = service.confirm(songId, ownerId);

		assertThat(confirmed.status()).isEqualTo(SongStatus.CONFIRMED);
	}

	@Test
	void 일반_멤버는_곡을_확정할_수_없다() {
		Long songId = service.add(band.getId(), memberId, manual(null)).id();
		em.flush();

		assertThatThrownBy(() -> service.confirm(songId, memberId)).isInstanceOf(ForbiddenException.class)
			.satisfies(ex -> assertThat(((ForbiddenException) ex).getCode()).isEqualTo("NOT_BAND_OWNER"));
	}

	// ── assignPart ───────────────────────────────────────

	@Test
	void WISHLIST_곡에는_파트를_배정할_수_없다() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();

		assertThatThrownBy(() -> service.assignPart(songId, partId, memberId, memberId, null))
			.isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("SONG_NOT_CONFIRMED"));
	}

	@Test
	void CONFIRMED_곡의_파트를_배정하고_해제한다() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();
		service.confirm(songId, ownerId);
		em.flush();

		SongResponse assigned = service.assignPart(songId, partId, memberId, memberId, null);
		assertThat(assigned.parts().getFirst().assignedUserId()).isEqualTo(memberId);

		SongResponse cleared = service.assignPart(songId, partId, memberId, null, null);
		assertThat(cleared.parts().getFirst().assignedUserId()).isNull();
	}

	@Test
	void 게스트를_파트에_배정하면_멤버_배정은_비워진다() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();
		service.confirm(songId, ownerId);
		Guest guest = em.persist(Fixtures.guest(band, "세션 기타"));
		em.flush();

		SongResponse assigned = service.assignPart(songId, partId, memberId, null, guest.getId());
		assertThat(assigned.parts().getFirst().assignedGuestId()).isEqualTo(guest.getId());
		assertThat(assigned.parts().getFirst().assignedName()).isEqualTo("세션 기타");

		SongResponse toMember = service.assignPart(songId, partId, memberId, memberId, null);
		assertThat(toMember.parts().getFirst().assignedGuestId()).isNull();
		assertThat(toMember.parts().getFirst().assignedUserId()).isEqualTo(memberId);
	}

	@Test
	void 멤버와_게스트를_동시에_배정하면_400() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		service.confirm(songId, ownerId);
		Guest guest = em.persist(Fixtures.guest(band, "세션"));
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();

		assertThatThrownBy(() -> service.assignPart(songId, partId, memberId, memberId, guest.getId()))
			.isInstanceOf(ValidationException.class)
			.satisfies(ex -> assertThat(((ValidationException) ex).getCode()).isEqualTo("PART_ASSIGN_AMBIGUOUS"));
	}

	@Test
	void 배정_대상이_밴드_멤버가_아니면_404() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		service.confirm(songId, ownerId);
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();
		Long outsiderId = em.persist(Fixtures.user("out2")).getId();

		assertThatThrownBy(() -> service.assignPart(songId, partId, memberId, outsiderId, null))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MEMBER_NOT_FOUND"));
	}

	// ── delete ───────────────────────────────────────────

	@Test
	void 관리자는_곡을_삭제하고_파트_투표도_함께_사라진다() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		service.vote(songId, memberId);
		em.flush();
		em.clear();

		service.delete(songId, ownerId);
		em.flush();
		em.clear();

		assertThat(songs.findById(songId)).isEmpty();
		assertThat(parts.findAllBySongId(songId)).isEmpty();
		assertThat(votes.existsBySongIdAndUserId(songId, memberId)).isFalse();
	}

	@Test
	void 일반_멤버는_곡을_삭제할_수_없다() {
		Long songId = service.add(band.getId(), memberId, manual(null)).id();
		em.flush();

		assertThatThrownBy(() -> service.delete(songId, memberId)).isInstanceOf(ForbiddenException.class);
	}

	// ── folder ───────────────────────────────────────────

	@Test
	void 곡을_같은_status_폴더로만_옮길_수_있고_멤버_누구나_가능() {
		Long songId = service.add(band.getId(), memberId, manual(null)).id();
		Long outsiderId = users.save(Fixtures.user("outsider")).getId();
		com.bandive.bandive.song.folder.SongFolder wishFolder = em
			.persist(com.bandive.bandive.song.folder.SongFolder.builder()
				.band(band)
				.name("커버")
				.status(SongStatus.WISHLIST)
				.position(0)
				.build());
		com.bandive.bandive.song.folder.SongFolder confFolder = em
			.persist(com.bandive.bandive.song.folder.SongFolder.builder()
				.band(band)
				.name("정규")
				.status(SongStatus.CONFIRMED)
				.position(0)
				.build());
		em.flush();

		// 밴드 멤버가 아니면 막힌다
		assertThatThrownBy(() -> service.moveToFolder(songId, outsiderId, wishFolder.getId()))
			.isInstanceOf(ForbiddenException.class);

		// 관리자가 아닌 일반 멤버도 옮길 수 있다
		assertThat(service.moveToFolder(songId, memberId, wishFolder.getId()).folderId()).isEqualTo(wishFolder.getId());

		// WISHLIST 곡을 CONFIRMED 폴더로는 못 옮긴다
		assertThatThrownBy(() -> service.moveToFolder(songId, memberId, confFolder.getId()))
			.isInstanceOf(com.bandive.bandive.common.exception.ValidationException.class);

		// null 이면 미분류
		assertThat(service.moveToFolder(songId, memberId, null).folderId()).isNull();
	}

	@Test
	void 그룹_안_곡_순서를_통째로_재지정한다() {
		Long a = service.add(band.getId(), memberId, manual(null)).id();
		Long b = service.add(band.getId(), memberId, manual(null)).id();
		Long c = service.add(band.getId(), memberId, manual(null)).id();
		em.flush();
		em.clear();

		// 처음엔 추가순 0,1,2
		assertThat(positionsById()).containsEntry(a, 0).containsEntry(b, 1).containsEntry(c, 2);

		service.reorder(band.getId(), memberId,
				new com.bandive.bandive.song.dto.SongOrderRequest(SongStatus.WISHLIST, null, List.of(c, a, b)));
		em.flush();
		em.clear();

		assertThat(positionsById()).containsEntry(c, 0).containsEntry(a, 1).containsEntry(b, 2);

		// 그룹의 곡 집합과 다르면 거부
		assertThatThrownBy(() -> service.reorder(band.getId(), memberId,
				new com.bandive.bandive.song.dto.SongOrderRequest(SongStatus.WISHLIST, null, List.of(a, b))))
			.isInstanceOf(com.bandive.bandive.common.exception.ValidationException.class);
	}

	private java.util.Map<Long, Integer> positionsById() {
		return songs.findAllByBandId(band.getId())
			.stream()
			.collect(java.util.stream.Collectors.toMap(com.bandive.bandive.song.Song::getId,
					com.bandive.bandive.song.Song::getPosition));
	}

	@Test
	void 승격하면_폴더에서_빠진다() {
		Long songId = service.add(band.getId(), memberId, manual(null)).id();
		com.bandive.bandive.song.folder.SongFolder folder = em
			.persist(com.bandive.bandive.song.folder.SongFolder.builder()
				.band(band)
				.name("커버")
				.status(SongStatus.WISHLIST)
				.position(0)
				.build());
		em.flush();
		service.moveToFolder(songId, ownerId, folder.getId());
		em.flush();

		assertThat(service.confirm(songId, ownerId).folderId()).isNull();
	}

}
