package com.bandive.bandive.schedule.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.auth.CurrentUser;
import com.bandive.bandive.auth.UserPrincipal;
import com.bandive.bandive.schedule.dto.AttendanceRequest;
import com.bandive.bandive.schedule.dto.ScheduleCreateRequest;
import com.bandive.bandive.schedule.dto.ScheduleResponse;
import com.bandive.bandive.schedule.dto.ScheduleUpdateRequest;
import com.bandive.bandive.schedule.service.ScheduleService;

@RestController
public class ScheduleController {

	private final ScheduleService scheduleService;

	public ScheduleController(ScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}

	@GetMapping("/api/bands/{bandId}/schedules")
	public List<ScheduleResponse> list(@PathVariable Long bandId, @AuthenticationPrincipal UserPrincipal principal) {
		Long userId = principal != null ? principal.getId() : null;
		return scheduleService.list(bandId, userId);
	}

	@PostMapping("/api/bands/{bandId}/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@bandGuard.isMember(#bandId)")
	public ScheduleResponse create(@PathVariable Long bandId, @CurrentUser Long userId,
			@Valid @RequestBody ScheduleCreateRequest request) {
		return scheduleService.create(bandId, userId, request);
	}

	@PatchMapping("/api/schedules/{scheduleId}")
	public ScheduleResponse update(@PathVariable Long scheduleId, @CurrentUser Long userId,
			@Valid @RequestBody ScheduleUpdateRequest request) {
		return scheduleService.update(scheduleId, userId, request);
	}

	@DeleteMapping("/api/schedules/{scheduleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long scheduleId, @CurrentUser Long userId) {
		scheduleService.delete(scheduleId, userId);
	}

	@PostMapping("/api/schedules/{scheduleId}/attendance")
	public ScheduleResponse setAttendance(@PathVariable Long scheduleId, @CurrentUser Long userId,
			@Valid @RequestBody AttendanceRequest request) {
		return scheduleService.setAttendance(scheduleId, userId, request.status());
	}

}
