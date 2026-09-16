package com.bandive.bandive.invite.dto;

/**
 * 초대 코드 미리보기 (가입 전, 공개). 초대 링크를 받은 사람이 참여 여부를 결정하기 전에 보여줄 밴드 요약.
 */
public record InvitePreviewResponse(String code, Long bandId, String bandName, String description, String logoUrl,
		long memberCount) {
}
