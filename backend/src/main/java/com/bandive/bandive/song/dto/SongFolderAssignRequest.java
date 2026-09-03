package com.bandive.bandive.song.dto;

/** 곡을 폴더로 이동. {@code folderId} 가 null 이면 미분류로 뺀다. */
public record SongFolderAssignRequest(Long folderId) {
}
