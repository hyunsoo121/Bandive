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
	private UserRepository users;

	@Autowired
	private TestEntityManager em;

	private SongService service;

	private Band band;

	private Long ownerId;

	private Long memberId;

	@BeforeEach
	void setUp() {
		service = new SongService(songs, parts, votes, bands, bandMembers, users, new StubMusicSearchService());
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
		return new SongCreateRequest("곡", "아티스트", SongSourceType.MANUAL, null, "메모", null, sessions);
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
		SongCreateRequest req = new SongCreateRequest("곡", "a", SongSourceType.SEARCH, "  ", null, null, null);

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
	void 밴드장은_곡을_확정한다() {
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

		assertThatThrownBy(() -> service.assignPart(songId, partId, memberId, memberId))
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

		SongResponse assigned = service.assignPart(songId, partId, memberId, memberId);
		assertThat(assigned.parts().getFirst().assignedUserId()).isEqualTo(memberId);

		SongResponse cleared = service.assignPart(songId, partId, memberId, null);
		assertThat(cleared.parts().getFirst().assignedUserId()).isNull();
	}

	@Test
	void 배정_대상이_밴드_멤버가_아니면_404() {
		Long songId = service.add(band.getId(), memberId, manual(List.of(new SessionSlot("GUITAR", 1)))).id();
		service.confirm(songId, ownerId);
		em.flush();
		Long partId = parts.findAllBySongId(songId).getFirst().getId();
		Long outsiderId = em.persist(Fixtures.user("out2")).getId();

		assertThatThrownBy(() -> service.assignPart(songId, partId, memberId, outsiderId))
			.isInstanceOf(NotFoundException.class)
			.satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MEMBER_NOT_FOUND"));
	}

	// ── delete ───────────────────────────────────────────

	@Test
	void 밴드장은_곡을_삭제하고_파트_투표도_함께_사라진다() {
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

}
