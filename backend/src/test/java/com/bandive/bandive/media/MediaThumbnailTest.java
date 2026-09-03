package com.bandive.bandive.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaThumbnailTest {

	@Test
	void 유튜브_여러_형태의_URL_에서_영상id_를_뽑는다() {
		String expected = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg";
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ")).isEqualTo(expected);
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
			.isEqualTo(expected);
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, "https://youtube.com/shorts/dQw4w9WgXcQ"))
			.isEqualTo(expected);
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, "https://www.youtube.com/embed/dQw4w9WgXcQ"))
			.isEqualTo(expected);
	}

	@Test
	void 구글드라이브_URL_에서_파일id_를_뽑는다() {
		assertThat(MediaThumbnail.of(MediaPlatform.GOOGLE_DRIVE, "https://drive.google.com/file/d/1AbC_dEfGhIjK/view"))
			.isEqualTo("https://drive.google.com/thumbnail?id=1AbC_dEfGhIjK&sz=w400");
		assertThat(MediaThumbnail.of(MediaPlatform.GOOGLE_DRIVE, "https://drive.google.com/open?id=1AbC_dEfGhIjK"))
			.isEqualTo("https://drive.google.com/thumbnail?id=1AbC_dEfGhIjK&sz=w400");
	}

	@Test
	void id_를_못_뽑거나_OTHER_면_null() {
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, "https://youtube.com/")).isNull();
		assertThat(MediaThumbnail.of(MediaPlatform.OTHER, "https://vimeo.com/12345")).isNull();
		assertThat(MediaThumbnail.of(MediaPlatform.YOUTUBE, null)).isNull();
	}

}
