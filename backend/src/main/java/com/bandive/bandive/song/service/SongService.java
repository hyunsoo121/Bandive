package com.bandive.bandive.song.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ConflictException;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.common.security.BandAccessGuard;
import com.bandive.bandive.guest.Guest;
import com.bandive.bandive.guest.GuestRepository;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongPart;
import com.bandive.bandive.song.SongPartRepository;
import com.bandive.bandive.song.SongRepository;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.Vote;
import com.bandive.bandive.song.VoteRepository;
import com.bandive.bandive.song.dto.SongCreateRequest;
import com.bandive.bandive.song.dto.SongCreateRequest.SessionSlot;
import com.bandive.bandive.song.dto.SongOrderRequest;
import com.bandive.bandive.song.dto.SongResponse;
import com.bandive.bandive.song.dto.SongUpdateRequest;
import com.bandive.bandive.song.dto.TrackSearchResult;
import com.bandive.bandive.song.dto.VoteResult;
import com.bandive.bandive.song.folder.SongFolder;
import com.bandive.bandive.song.folder.SongFolderRepository;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class SongService {

	private final SongRepository songs;

	private final SongPartRepository parts;

	private final VoteRepository votes;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	private final GuestRepository guests;

	private final UserRepository users;

	private final SongFolderRepository folders;

	private final MusicSearchService musicSearch;

	private final BandAccessGuard bandAccess;

	public SongService(SongRepository songs, SongPartRepository parts, VoteRepository votes, BandRepository bands,
			BandMemberRepository bandMembers, GuestRepository guests, UserRepository users,
			SongFolderRepository folders, MusicSearchService musicSearch, BandAccessGuard bandAccess) {
		this.songs = songs;
		this.parts = parts;
		this.votes = votes;
		this.bands = bands;
		this.bandMembers = bandMembers;
		this.guests = guests;
		this.users = users;
		this.folders = folders;
		this.musicSearch = musicSearch;
		this.bandAccess = bandAccess;
	}

	public List<TrackSearchResult> search(String query) {
		return musicSearch.search(query);
	}

	/**
	 * 곡 목록. status null 이면 전체. currentUserId null(비회원) 이면 votedByMe 는 전부 false. 밴드 공개범위
	 * 게이트 적용.
	 */
	public List<SongResponse> list(Long bandId, SongStatus status, Long currentUserId) {
		bandAccess.requireCanViewContent(bandId, currentUserId);
		List<Song> found = songs.findAllForBand(bandId, status);
		if (found.isEmpty()) {
			return List.of();
		}
		List<Long> ids = found.stream().map(Song::getId).toList();

		Map<Long, Long> voteCounts = new HashMap<>();
		for (Object[] row : votes.countBySongIds(ids)) {
			voteCounts.put((Long) row[0], (Long) row[1]);
		}
		Set<Long> votedIds = currentUserId == null ? Set.of()
				: new HashSet<>(votes.findVotedSongIds(currentUserId, ids));

		return found.stream()
			.map(song -> SongResponse.from(song, voteCounts.getOrDefault(song.getId(), 0L),
					votedIds.contains(song.getId())))
			.toList();
	}

	@Transactional
	public SongResponse add(Long bandId, Long userId, SongCreateRequest request) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		requireMember(bandId, userId);

		if (request.sourceType() == SongSourceType.SEARCH && !StringUtils.hasText(request.externalTrackId())) {
			throw new ValidationException("EXTERNAL_TRACK_ID_REQUIRED", "검색으로 추가하려면 트랙 식별자가 필요합니다.");
		}

		SongStatus status = request.status() != null ? request.status() : SongStatus.WISHLIST;
		if (status == SongStatus.CONFIRMED) {
			// 합주곡으로 바로 등록(승격 단계 생략)은 관리자만 — confirm() 과 동일한 권한.
			requireOwner(bandId, userId);
		}

		User addedBy = users.findById(userId)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

		Song song = Song.builder()
			.band(band)
			.addedBy(addedBy)
			.title(request.title().trim())
			.artist(trimToNull(request.artist()))
			.status(status)
			.sourceType(request.sourceType())
			.externalTrackId(trimToNull(request.externalTrackId()))
			.artworkUrl(trimToNull(request.artworkUrl()))
			.memo(request.memo())
			.referenceVideoUrl(trimToNull(request.referenceVideoUrl()))
			.build();

		if (request.sessions() != null) {
			for (SessionSlot slot : request.sessions()) {
				for (int index = 1; index <= slot.count(); index++) {
					song.addPart(SongPart.builder().instrument(slot.instrument().trim()).partIndex(index).build());
				}
			}
		}
		// 새 곡은 항상 미분류 — 그 status 그룹 맨 끝에 놓는다.
		song.moveToPosition(songs.countByBandIdAndStatusAndFolderIsNull(bandId, status));
		songs.save(song);
		return SongResponse.from(song, 0L, false);
	}

	@Transactional
	public VoteResult vote(Long songId, Long userId) {
		Song song = findSong(songId);
		requireMember(song.getBand().getId(), userId);
		if (!votes.existsBySongIdAndUserId(songId, userId)) {
			votes.save(Vote.builder().song(song).user(users.getReferenceById(userId)).build());
		}
		return new VoteResult(votes.countBySongId(songId), true);
	}

	@Transactional
	public VoteResult unvote(Long songId, Long userId) {
		Song song = findSong(songId);
		requireMember(song.getBand().getId(), userId);
		votes.deleteBySongIdAndUserId(songId, userId);
		return new VoteResult(votes.countBySongId(songId), false);
	}

	/** WISHLIST → CONFIRMED (관리자). */
	@Transactional
	public SongResponse confirm(Long songId, Long userId) {
		Song song = findSongWithDetails(songId);
		requireOwner(song.getBand().getId(), userId);
		song.confirm();
		return toResponse(song, userId);
	}

	/**
	 * 파트 배정/해제 (밴드 멤버 누구나). 곡이 CONFIRMED 여야 한다. 실멤버(targetUserId)와 게스트(targetGuestId)는 최대
	 * 하나.
	 */
	@Transactional
	public SongResponse assignPart(Long songId, Long partId, Long actorUserId, Long targetUserId, Long targetGuestId) {
		Song song = findSongWithDetails(songId);
		Long bandId = song.getBand().getId();
		requireMember(bandId, actorUserId);
		if (!song.isConfirmed()) {
			throw new ConflictException("SONG_NOT_CONFIRMED", "확정된 합주곡만 파트를 배정할 수 있습니다.");
		}
		if (targetUserId != null && targetGuestId != null) {
			throw new ValidationException("PART_ASSIGN_AMBIGUOUS", "멤버와 게스트를 동시에 배정할 수 없습니다.");
		}

		SongPart part = parts.findByIdAndSongId(partId, songId)
			.orElseThrow(() -> new NotFoundException("PART_NOT_FOUND", "해당 파트를 찾을 수 없습니다."));

		if (targetUserId != null) {
			BandMember target = bandMembers.findByBandIdAndUserId(bandId, targetUserId)
				.orElseThrow(() -> new NotFoundException("MEMBER_NOT_FOUND", "배정 대상이 이 밴드의 멤버가 아닙니다."));
			part.assignTo(target);
		}
		else if (targetGuestId != null) {
			Guest target = guests.findByIdAndBandId(targetGuestId, bandId)
				.orElseThrow(() -> new NotFoundException("GUEST_NOT_FOUND", "배정 대상 게스트를 찾을 수 없습니다."));
			part.assignTo(target);
		}
		else {
			part.unassign();
		}
		return toResponse(song, actorUserId);
	}

	/**
	 * 부분 수정 (제목/아티스트/메모/참고영상) — 등록자 본인 또는 관리자. 위시리스트·합주곡 상태 모두 가능. null 필드는 유지.
	 */
	@Transactional
	public SongResponse update(Long songId, Long userId, SongUpdateRequest request) {
		Song song = findSongWithDetails(songId);
		requireProposerOrOwner(song, userId);

		String title = request.title() != null ? request.title().trim() : song.getTitle();
		if (title.isEmpty()) {
			throw new ValidationException("TITLE_REQUIRED", "곡 제목은 필수입니다");
		}
		String artist = request.artist() != null ? trimToNull(request.artist()) : song.getArtist();
		String memo = request.memo() != null ? request.memo() : song.getMemo();
		String referenceVideoUrl = request.referenceVideoUrl() != null ? trimToNull(request.referenceVideoUrl())
				: song.getReferenceVideoUrl();

		song.updateDetails(title, artist, memo, referenceVideoUrl);
		if (request.sessions() != null) {
			applySessions(song, request.sessions());
		}
		return toResponse(song, userId);
	}

	/**
	 * 세션 구성을 target 으로 맞춘다(등록 시와 같은 "전체 목록" 의미 — 목록에 없는 악기는 0). 인원이 늘면 빈 슬롯을 추가하고, 줄면 배정
	 * 없는 슬롯부터 지운다. 줄이려는 만큼 배정 없는 슬롯이 없으면 거부.
	 */
	private void applySessions(Song song, List<SessionSlot> target) {
		Map<String, Integer> targetCounts = new LinkedHashMap<>();
		for (SessionSlot slot : target) {
			String instrument = slot.instrument().trim();
			targetCounts.merge(instrument, slot.count(), Integer::sum);
		}

		Map<String, List<SongPart>> existingByInstrument = new HashMap<>();
		for (SongPart part : song.getParts()) {
			existingByInstrument.computeIfAbsent(part.getInstrument(), k -> new ArrayList<>()).add(part);
		}

		Set<String> instruments = new LinkedHashSet<>();
		instruments.addAll(existingByInstrument.keySet());
		instruments.addAll(targetCounts.keySet());

		List<SongPart> toRemove = new ArrayList<>();
		for (String instrument : instruments) {
			List<SongPart> existing = existingByInstrument.getOrDefault(instrument, List.of());
			int currentCount = existing.size();
			int targetCount = targetCounts.getOrDefault(instrument, 0);

			if (targetCount < currentCount) {
				int need = currentCount - targetCount;
				List<SongPart> removable = existing.stream()
					.filter(p -> !p.isAssigned())
					.sorted(Comparator.comparing(SongPart::getPartIndex).reversed())
					.limit(need)
					.toList();
				if (removable.size() < need) {
					throw new ConflictException("SESSION_SLOT_ASSIGNED",
							"'" + instrument + "' 에 이미 배정된 멤버가 있어 그만큼 줄일 수 없습니다. 먼저 배정을 해제해 주세요.");
				}
				toRemove.addAll(removable);
			}
			else if (targetCount > currentCount) {
				int maxIndex = existing.stream().mapToInt(SongPart::getPartIndex).max().orElse(0);
				for (int i = 1; i <= targetCount - currentCount; i++) {
					song.addPart(SongPart.builder().instrument(instrument).partIndex(maxIndex + i).build());
				}
			}
		}
		if (!toRemove.isEmpty()) {
			song.removeParts(toRemove);
		}
	}

	/** 곡 삭제 (관리자). song_parts·votes 는 cascade. */
	@Transactional
	public void delete(Long songId, Long userId) {
		Song song = findSong(songId);
		requireOwner(song.getBand().getId(), userId);
		songs.delete(song);
	}

	/** 곡을 폴더로 이동 (밴드 멤버 누구나). folderId 가 null 이면 미분류. 폴더는 곡과 같은 밴드·같은 status 여야 한다. */
	@Transactional
	public SongResponse moveToFolder(Long songId, Long userId, Long folderId) {
		Song song = findSongWithDetails(songId);
		Long bandId = song.getBand().getId();
		requireMember(bandId, userId);
		if (folderId == null) {
			// 이미 미분류가 아니었으면 미분류 그룹 맨 끝으로.
			int position = songs.countByBandIdAndStatusAndFolderIsNull(bandId, song.getStatus());
			song.moveToFolder(null);
			song.moveToPosition(position);
		}
		else {
			SongFolder folder = folders.findById(folderId)
				.orElseThrow(() -> new NotFoundException("FOLDER_NOT_FOUND", "폴더를 찾을 수 없습니다."));
			if (!folder.getBand().getId().equals(bandId)) {
				throw new ValidationException("FOLDER_BAND_MISMATCH", "다른 밴드의 폴더로는 옮길 수 없습니다.");
			}
			if (folder.getStatus() != song.getStatus()) {
				throw new ValidationException("FOLDER_STATUS_MISMATCH", "위시리스트/합주곡 구분이 다른 폴더입니다.");
			}
			int position = songs.countByFolderId(folderId);
			song.moveToFolder(folder);
			song.moveToPosition(position);
		}
		return toResponse(song, userId);
	}

	/** 한 그룹(status × 폴더/미분류) 안에서 곡 순서를 통째로 다시 지정 (밴드 멤버 누구나). */
	@Transactional
	public void reorder(Long bandId, Long userId, SongOrderRequest request) {
		requireMember(bandId, userId);
		List<Song> current = (request.folderId() == null)
				? songs.findByBandIdAndStatusAndFolderIsNullOrderByPositionAsc(bandId, request.status()) : songs
					.findByBandIdAndStatusAndFolderIdOrderByPositionAsc(bandId, request.status(), request.folderId());
		Set<Long> currentIds = current.stream().map(Song::getId).collect(HashSet::new, Set::add, Set::addAll);
		if (currentIds.size() != request.songIds().size() || !currentIds.containsAll(request.songIds())) {
			throw new ValidationException("SONG_ORDER_MISMATCH", "곡 순서 목록이 현재 그룹의 곡과 일치하지 않습니다.");
		}
		for (int i = 0; i < request.songIds().size(); i++) {
			Long id = request.songIds().get(i);
			int position = i;
			current.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow().moveToPosition(position);
		}
	}

	private Song findSong(Long songId) {
		return songs.findById(songId).orElseThrow(() -> new NotFoundException("SONG_NOT_FOUND", "곡을 찾을 수 없습니다."));
	}

	private Song findSongWithDetails(Long songId) {
		return songs.findByIdWithDetails(songId)
			.orElseThrow(() -> new NotFoundException("SONG_NOT_FOUND", "곡을 찾을 수 없습니다."));
	}

	private SongResponse toResponse(Song song, Long currentUserId) {
		boolean votedByMe = currentUserId != null && votes.existsBySongIdAndUserId(song.getId(), currentUserId);
		return SongResponse.from(song, votes.countBySongId(song.getId()), votedByMe);
	}

	private void requireMember(Long bandId, Long userId) {
		if (!bandMembers.existsByBandIdAndUserId(bandId, userId)) {
			throw new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다.");
		}
	}

	private void requireProposerOrOwner(Song song, Long userId) {
		if (!song.getAddedBy().getId().equals(userId)) {
			requireOwner(song.getBand().getId(), userId);
		}
	}

	private void requireOwner(Long bandId, Long userId) {
		BandMember member = bandMembers.findByBandIdAndUserId(bandId, userId)
			.orElseThrow(() -> new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다."));
		if (member.getRole() != BandRole.OWNER) {
			throw new ForbiddenException("NOT_BAND_OWNER", "관리자만 할 수 있습니다.");
		}
	}

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

}
