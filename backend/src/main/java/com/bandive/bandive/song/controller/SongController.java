package com.bandive.bandive.song.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.dto.PartAssignRequest;
import com.bandive.bandive.song.dto.SongCreateRequest;
import com.bandive.bandive.song.dto.SongResponse;
import com.bandive.bandive.song.dto.TrackSearchResult;
import com.bandive.bandive.song.dto.VoteResult;
import com.bandive.bandive.song.service.SongService;

@RestController
public class SongController {

	private final SongService songService;

	public SongController(SongService songService) {
		this.songService = songService;
	}

	@GetMapping("/api/songs/search")
	public List<TrackSearchResult> search(@RequestParam String q) {
		return songService.search(q);
	}

	@GetMapping("/api/bands/{bandId}/songs")
	public List<SongResponse> list(@PathVariable Long bandId,
			@RequestParam(name = "status", required = false) SongStatus status,
			@AuthenticationPrincipal UserPrincipal principal) {
		// 비회원도 볼 수 있는 GET — 익명이면 principal 이 null 이라 votedByMe 는 전부 false
		Long userId = principal != null ? principal.getId() : null;
		return songService.list(bandId, status, userId);
	}

	@PostMapping("/api/bands/{bandId}/songs")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@bandGuard.isMember(#bandId)")
	public SongResponse add(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody SongCreateRequest request) {
		return songService.add(bandId, userId, request);
	}

	@PostMapping("/api/songs/{songId}/vote")
	public VoteResult vote(@PathVariable Long songId, @CurrentUser Long userId) {
		return songService.vote(songId, userId);
	}

	@DeleteMapping("/api/songs/{songId}/vote")
	public VoteResult unvote(@PathVariable Long songId, @CurrentUser Long userId) {
		return songService.unvote(songId, userId);
	}

	@PatchMapping("/api/songs/{songId}/confirm")
	public SongResponse confirm(@PathVariable Long songId, @CurrentUser Long userId) {
		return songService.confirm(songId, userId);
	}

	@PutMapping("/api/songs/{songId}/parts/{partId}/assign")
	public SongResponse assignPart(@PathVariable Long songId, @PathVariable Long partId, @CurrentUser Long userId,
			@RequestBody(required = false) PartAssignRequest request) {
		Long targetUserId = request == null ? null : request.userId();
		return songService.assignPart(songId, partId, userId, targetUserId);
	}

	@DeleteMapping("/api/songs/{songId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long songId, @CurrentUser Long userId) {
		songService.delete(songId, userId);
	}

}
