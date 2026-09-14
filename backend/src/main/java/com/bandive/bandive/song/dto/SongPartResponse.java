package com.bandive.bandive.song.dto;

import com.bandive.bandive.song.SongPart;

/**
 * 세션 슬롯. 배정 대상은 실멤버(assignedUserId) 또는 게스트(assignedGuestId) 중 최대 하나. assignedName 은 둘 중
 * 배정된 쪽의 표시 이름.
 */
public record SongPartResponse(Long id, String instrument, int partIndex, Long assignedUserId, Long assignedGuestId,
		String assignedName) {

	public static SongPartResponse from(SongPart part) {
		Long userId = null;
		Long guestId = null;
		String name = null;
		if (part.getAssignedMember() != null) {
			userId = part.getAssignedMember().getUser().getId();
			name = part.getAssignedMember().getUser().getNickname();
		}
		else if (part.getAssignedGuest() != null) {
			guestId = part.getAssignedGuest().getId();
			name = part.getAssignedGuest().getName();
		}
		return new SongPartResponse(part.getId(), part.getInstrument(), part.getPartIndex(), userId, guestId, name);
	}

}
