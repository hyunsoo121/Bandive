package com.bandive.bandive.guest.controller;

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
import com.bandive.bandive.guest.dto.GuestRequest;
import com.bandive.bandive.guest.dto.GuestResponse;
import com.bandive.bandive.guest.dto.GuestSessionRequest;
import com.bandive.bandive.guest.service.GuestService;

@RestController
public class GuestController {

	private final GuestService guestService;

	public GuestController(GuestService guestService) {
		this.guestService = guestService;
	}

	/** 게스트 목록 — 밴드 공개범위 게이트 적용. 이름순. */
	@GetMapping("/api/bands/{bandId}/guests")
	public List<GuestResponse> list(@PathVariable Long bandId, @AuthenticationPrincipal UserPrincipal principal) {
		return guestService.list(bandId, principal != null ? principal.getId() : null);
	}

	@PostMapping("/api/bands/{bandId}/guests")
	@ResponseStatus(HttpStatus.CREATED)
	public GuestResponse create(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody GuestRequest request) {
		return guestService.create(bandId, userId, request.name());
	}

	@PatchMapping("/api/bands/{bandId}/guests/{guestId}")
	public GuestResponse rename(@PathVariable Long bandId, @PathVariable Long guestId, @CurrentUser Long userId,
			@Valid @RequestBody GuestRequest request) {
		return guestService.rename(bandId, guestId, userId, request.name());
	}

	/** 게스트 세션 설정 (악기 또는 "관객", null·빈 값이면 미지정). */
	@PutMapping("/api/bands/{bandId}/guests/{guestId}/session")
	public GuestResponse setSession(@PathVariable Long bandId, @PathVariable Long guestId, @CurrentUser Long userId,
			@Valid @RequestBody(required = false) GuestSessionRequest request) {
		return guestService.setSession(bandId, guestId, userId, request == null ? null : request.session());
	}

	@DeleteMapping("/api/bands/{bandId}/guests/{guestId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long bandId, @PathVariable Long guestId, @CurrentUser Long userId) {
		guestService.delete(bandId, guestId, userId);
	}

}
