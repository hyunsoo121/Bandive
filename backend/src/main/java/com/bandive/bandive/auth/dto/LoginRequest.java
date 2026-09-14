package com.bandive.bandive.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 이메일 로그인. */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
