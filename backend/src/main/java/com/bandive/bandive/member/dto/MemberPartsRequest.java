package com.bandive.bandive.member.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 멤버 파트 전체 교체. null 또는 빈 리스트면 모든 파트 해제.
 */
public record MemberPartsRequest(
		@Size(max = 5, message = "파트는 최대 5개까지 지정할 수 있습니다") List<@NotBlank(message = "파트 값이 비어 있습니다") @Size(max = 20,
				message = "파트는 20자 이내여야 합니다") String> parts) {
}
