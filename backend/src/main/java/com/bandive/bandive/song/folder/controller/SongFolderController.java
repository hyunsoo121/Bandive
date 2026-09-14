package com.bandive.bandive.song.folder.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.song.folder.dto.CreateFolderRequest;
import com.bandive.bandive.song.folder.dto.FolderOrderRequest;
import com.bandive.bandive.song.folder.dto.RenameFolderRequest;
import com.bandive.bandive.song.folder.dto.SongFolderResponse;
import com.bandive.bandive.song.folder.service.SongFolderService;

@RestController
public class SongFolderController {

	private final SongFolderService folderService;

	public SongFolderController(SongFolderService folderService) {
		this.folderService = folderService;
	}

	/** 폴더 목록 — 밴드 공개범위 게이트 적용. 위시/합주 폴더 모두, status·position 순. */
	@GetMapping("/api/bands/{bandId}/song-folders")
	public List<SongFolderResponse> list(@PathVariable Long bandId, @AuthenticationPrincipal UserPrincipal principal) {
		return folderService.list(bandId, principal != null ? principal.getId() : null);
	}

	@PostMapping("/api/bands/{bandId}/song-folders")
	@ResponseStatus(HttpStatus.CREATED)
	public SongFolderResponse create(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody CreateFolderRequest request) {
		return folderService.create(bandId, userId, request);
	}

	/** 한 status 안에서 폴더 순서 재지정 (관리자). */
	@PutMapping("/api/bands/{bandId}/song-folders/order")
	public List<SongFolderResponse> reorder(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody FolderOrderRequest request) {
		return folderService.reorder(bandId, userId, request);
	}

	@PatchMapping("/api/song-folders/{folderId}")
	public SongFolderResponse rename(@PathVariable Long folderId, @CurrentUser Long userId,
			@Valid @RequestBody RenameFolderRequest request) {
		return folderService.rename(folderId, userId, request.name());
	}

	@DeleteMapping("/api/song-folders/{folderId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long folderId, @CurrentUser Long userId) {
		folderService.delete(folderId, userId);
	}

}
