package com.bandive.bandive.song.dto;

import com.bandive.bandive.song.SongPart;

public record SongPartResponse(Long id, String instrument, int partIndex, Long assignedUserId,
		String assignedNickname) {

	public static SongPartResponse from(SongPart part) {
		Long userId = null;
		String nickname = null;
		if (part.getAssignedMember() != null) {
			userId = part.getAssignedMember().getUser().getId();
			nickname = part.getAssignedMember().getUser().getNickname();
		}
		return new SongPartResponse(part.getId(), part.getInstrument(), part.getPartIndex(), userId, nickname);
	}

}
