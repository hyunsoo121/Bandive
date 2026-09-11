package com.bandive.bandive.song.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.bandive.bandive.song.SongStatus;

/** 폴더 생성 — 목록 끝에 추가된다. */
public record CreateFolderRequest(@NotBlank @Size(max = 60) String name, @NotNull SongStatus status) {
}
