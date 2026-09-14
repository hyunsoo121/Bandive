package com.bandive.bandive.band.dto;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(@NotNull(message = "위임할 사용자 id 는 필수입니다") Long userId) {
}
