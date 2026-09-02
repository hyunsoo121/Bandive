package com.bandive.bandive.media.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.media.dto.MediaCreateRequest;
import com.bandive.bandive.media.dto.MediaResponse;
import com.bandive.bandive.media.dto.VisibilityRequest;
import com.bandive.bandive.media.service.MediaService;

@RestController
public class MediaController {

	private final MediaService mediaService;

	public MediaController(MediaService mediaService) {
		this.mediaService = mediaService;
	}

	@GetMapping("/api/bands/{bandId}/media")
	public List<MediaResponse> list(@PathVariable Long bandId,
			@RequestParam(name = "scheduleId", required = false) Long scheduleId,
			@AuthenticationPrincipal UserPrincipal principal) {
		Long userId = principal != null ? principal.getId() : null;
		return mediaService.list(bandId, scheduleId, userId);
	}

	@PostMapping("/api/bands/{bandId}/media")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@bandGuard.isMember(#bandId)")
	public MediaResponse create(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody MediaCreateRequest request) {
		return mediaService.create(bandId, userId, request);
	}

	@PatchMapping("/api/media/{mediaId}/visibility")
	public MediaResponse changeVisibility(@PathVariable Long mediaId, @CurrentUser Long userId,
			@Valid @RequestBody VisibilityRequest request) {
		return mediaService.changeVisibility(mediaId, userId, request.visibility());
	}

	@DeleteMapping("/api/media/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long mediaId, @CurrentUser Long userId) {
		mediaService.delete(mediaId, userId);
	}

}
