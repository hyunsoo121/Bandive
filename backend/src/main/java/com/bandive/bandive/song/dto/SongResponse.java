package com.bandive.bandive.song.dto;

import java.time.Instant;
import java.util.List;

import com.bandive.bandive.song.Song;
import com.bandive.bandive.song.SongSourceType;
import com.bandive.bandive.song.SongStatus;

public record SongResponse(Long id, Long bandId, String title, String artist, SongStatus status,
		SongSourceType sourceType, String externalTrackId, String memo, String referenceVideoUrl, Long addedByUserId,
		String addedByNickname, long voteCount, boolean votedByMe, List<SongPartResponse> parts, Instant createdAt) {

	public static SongResponse from(Song song, long voteCount, boolean votedByMe) {
		List<SongPartResponse> parts = song.getParts().stream().sorted((a, b) -> {
			int byInstrument = a.getInstrument().compareTo(b.getInstrument());
			return byInstrument != 0 ? byInstrument : Integer.compare(a.getPartIndex(), b.getPartIndex());
		}).map(SongPartResponse::from).toList();
		return new SongResponse(song.getId(), song.getBand().getId(), song.getTitle(), song.getArtist(),
				song.getStatus(), song.getSourceType(), song.getExternalTrackId(), song.getMemo(),
				song.getReferenceVideoUrl(), song.getAddedBy().getId(), song.getAddedBy().getNickname(), voteCount,
				votedByMe, parts, song.getCreatedAt());
	}

}
