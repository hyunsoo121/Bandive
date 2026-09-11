package com.bandive.bandive.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bandive.bandive.user.dto.UserProfileResponse;
import com.bandive.bandive.user.service.UserService;

@RestController
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/** 다른 사람 프로필 조회 (공개 GET). */
	@GetMapping("/api/users/{userId}")
	public UserProfileResponse get(@PathVariable Long userId) {
		return userService.getProfile(userId);
	}

}
