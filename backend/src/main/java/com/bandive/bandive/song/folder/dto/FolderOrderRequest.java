package com.bandive.bandive.song.folder.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.bandive.bandive.song.SongStatus;

/**
 * 한 status(위시/합주) 안에서 폴더 순서를 통째로 다시 지정. {@code folderIds} 는 그 status 의 모든 폴더 id 를 새 순서대로.
 */
public record FolderOrderRequest(@NotNull SongStatus status, @NotNull List<Long> folderIds) {
}
