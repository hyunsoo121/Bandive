package com.bandive.bandive.band.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.band.dto.BandCreateRequest;
import com.bandive.bandive.band.dto.BandResponse;
import com.bandive.bandive.band.dto.BandUpdateRequest;
import com.bandive.bandive.band.dto.TransferOwnershipRequest;
import com.bandive.bandive.band.service.BandService;

@RestController
@RequestMapping("/api/bands")
public class BandController {

	private final BandService bandService;

	public BandController(BandService bandService) {
		this.bandService = bandService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BandResponse create(@CurrentUser Long userId, @Valid @RequestBody BandCreateRequest request) {
		return bandService.create(userId, request);
	}

	@GetMapping("/my")
	public List<BandResponse> myBands(@CurrentUser Long userId) {
		return bandService.myBands(userId);
	}

	@GetMapping("/{bandId}")
	public BandResponse get(@PathVariable Long bandId) {
		return bandService.get(bandId);
	}

	@PatchMapping("/{bandId}")
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public BandResponse update(@PathVariable Long bandId, @Valid @RequestBody BandUpdateRequest request) {
		return bandService.update(bandId, request);
	}

	@PostMapping("/{bandId}/logo")
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public BandResponse uploadLogo(@PathVariable Long bandId, @RequestParam("file") MultipartFile file) {
		return bandService.updateLogo(bandId, file);
	}

	@PostMapping("/{bandId}/banner")
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public BandResponse uploadBanner(@PathVariable Long bandId, @RequestParam("file") MultipartFile file) {
		return bandService.updateBanner(bandId, file);
	}

	@PutMapping("/{bandId}/owner")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public void transferOwnership(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody TransferOwnershipRequest request) {
		bandService.transferOwnership(bandId, userId, request.userId());
	}

	@DeleteMapping("/{bandId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@bandGuard.isOwner(#bandId)")
	public void delete(@PathVariable Long bandId) {
		bandService.delete(bandId);
	}

}
