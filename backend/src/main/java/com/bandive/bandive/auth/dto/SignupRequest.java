package com.bandive.bandive.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이메일 회원가입. 비밀번호는 8자 이상 + 영문·숫자 포함.
 */
public record SignupRequest(@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
				message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다") String password,
		@NotBlank @Size(max = 50) String nickname) {
}
