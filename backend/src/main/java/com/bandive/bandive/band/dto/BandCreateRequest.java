package com.bandive.bandive.band.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BandCreateRequest(
		@NotBlank(message = "밴드 이름은 필수입니다") @Size(max = 100, message = "밴드 이름은 100자 이내여야 합니다") String name,
		@Size(max = 500, message = "소개는 500자 이내여야 합니다") String description) {
}
