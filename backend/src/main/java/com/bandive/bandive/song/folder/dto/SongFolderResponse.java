package com.bandive.bandive.song.folder.dto;

import com.bandive.bandive.song.SongStatus;
import com.bandive.bandive.song.folder.SongFolder;

public record SongFolderResponse(Long id, String name, SongStatus status, int position) {

	public static SongFolderResponse from(SongFolder folder) {
		return new SongFolderResponse(folder.getId(), folder.getName(), folder.getStatus(), folder.getPosition());
	}

}
