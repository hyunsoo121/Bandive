package com.bandive.bandive.song.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bandive.bandive.song.SongStatus;

/**
 * 한 그룹(= status × 폴더 또는 미분류) 안에서 곡 순서를 통째로 다시 지정. {@code songIds} 는 그 그룹의 모든 곡 id 를 새
 * 순서대로. {@code folderId} 가 null 이면 미분류 그룹.
 */
public record SongOrderRequest(@NotNull SongStatus status, Long folderId, @NotEmpty List<Long> songIds) {
}
