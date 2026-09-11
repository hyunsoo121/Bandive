package com.bandive.bandive.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 (LOCAL 계정만). 새 비밀번호 규칙은 회원가입과 동일 — 8자 이상 + 영문·숫자 포함.
 */
public record PasswordChangeRequest(@NotBlank(message = "현재 비밀번호를 입력해 주세요") String currentPassword,
		@NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
				message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다") String newPassword) {
}
