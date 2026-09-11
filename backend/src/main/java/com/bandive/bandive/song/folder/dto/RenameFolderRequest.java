package com.bandive.bandive.song.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameFolderRequest(@NotBlank @Size(max = 60) String name) {
}
