package com.bandive.bandive.song.folder.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.band.Band;
import com.bandive.bandive.band.BandRepository;
import com.bandive.bandive.common.exception.ForbiddenException;
import com.bandive.bandive.common.exception.NotFoundException;
import com.bandive.bandive.common.exception.ValidationException;
import com.bandive.bandive.member.BandMember;
import com.bandive.bandive.member.BandMemberRepository;
import com.bandive.bandive.member.BandRole;
import com.bandive.bandive.song.SongRepository;
import com.bandive.bandive.song.folder.SongFolder;
import com.bandive.bandive.song.folder.SongFolderRepository;
import com.bandive.bandive.song.folder.dto.CreateFolderRequest;
import com.bandive.bandive.song.folder.dto.FolderOrderRequest;
import com.bandive.bandive.song.folder.dto.SongFolderResponse;

/**
 * 곡 폴더 관리. 생성·이름변경·삭제·순서변경은 모두 밴드장만. 목록 조회는 공개.
 */
@Service
@Transactional(readOnly = true)
public class SongFolderService {

	private final SongFolderRepository folders;

	private final SongRepository songs;

	private final BandRepository bands;

	private final BandMemberRepository bandMembers;

	public SongFolderService(SongFolderRepository folders, SongRepository songs, BandRepository bands,
			BandMemberRepository bandMembers) {
		this.folders = folders;
		this.songs = songs;
		this.bands = bands;
		this.bandMembers = bandMembers;
	}

	public List<SongFolderResponse> list(Long bandId) {
		if (!bands.existsById(bandId)) {
			throw new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.");
		}
		return folders.findByBandIdOrderByStatusAscPositionAsc(bandId).stream().map(SongFolderResponse::from).toList();
	}

	@Transactional
	public SongFolderResponse create(Long bandId, Long userId, CreateFolderRequest request) {
		Band band = bands.findById(bandId).orElseThrow(() -> new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다."));
		requireOwner(bandId, userId);
		int position = folders.countByBandIdAndStatus(bandId, request.status());
		SongFolder folder = folders.save(SongFolder.builder()
			.band(band)
			.name(request.name().trim())
			.status(request.status())
			.position(position)
			.build());
		return SongFolderResponse.from(folder);
	}

	@Transactional
	public SongFolderResponse rename(Long folderId, Long userId, String name) {
		SongFolder folder = find(folderId);
		requireOwner(folder.getBand().getId(), userId);
		folder.rename(name.trim());
		return SongFolderResponse.from(folder);
	}

	@Transactional
	public void delete(Long folderId, Long userId) {
		SongFolder folder = find(folderId);
		requireOwner(folder.getBand().getId(), userId);
		songs.clearFolder(folderId); // 소속 곡들은 미분류로
		folders.delete(folder);
	}

	/** 한 status 안에서 폴더 순서를 통째로 재지정. folderIds 는 그 status 의 모든 폴더를 정확히 포함해야 한다. */
	@Transactional
	public List<SongFolderResponse> reorder(Long bandId, Long userId, FolderOrderRequest request) {
		requireOwner(bandId, userId);
		List<SongFolder> current = folders.findByBandIdAndStatusOrderByPositionAsc(bandId, request.status());
		Set<Long> currentIds = current.stream().map(SongFolder::getId).collect(HashSet::new, Set::add, Set::addAll);
		if (currentIds.size() != request.folderIds().size() || !currentIds.containsAll(request.folderIds())) {
			throw new ValidationException("FOLDER_ORDER_MISMATCH", "폴더 순서 목록이 현재 폴더와 일치하지 않습니다.");
		}
		for (int i = 0; i < request.folderIds().size(); i++) {
			Long id = request.folderIds().get(i);
			current.stream().filter(f -> f.getId().equals(id)).findFirst().orElseThrow().moveTo(i);
		}
		return folders.findByBandIdAndStatusOrderByPositionAsc(bandId, request.status())
			.stream()
			.map(SongFolderResponse::from)
			.toList();
	}

	private SongFolder find(Long folderId) {
		return folders.findById(folderId)
			.orElseThrow(() -> new NotFoundException("FOLDER_NOT_FOUND", "폴더를 찾을 수 없습니다."));
	}

	private void requireOwner(Long bandId, Long userId) {
		BandMember member = bandMembers.findByBandIdAndUserId(bandId, userId)
			.orElseThrow(() -> new ForbiddenException("NOT_A_MEMBER", "이 밴드의 멤버가 아닙니다."));
		if (member.getRole() != BandRole.OWNER) {
			throw new ForbiddenException("NOT_BAND_OWNER", "관리자만 할 수 있습니다.");
		}
	}

}
