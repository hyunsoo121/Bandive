package com.bandive.bandive.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaPlatformTest {

	@Test
	void 유튜브_URL_을_알아본다() {
		assertThat(MediaPlatform.detect("https://www.youtube.com/watch?v=abc")).isEqualTo(MediaPlatform.YOUTUBE);
		assertThat(MediaPlatform.detect("https://youtu.be/abc")).isEqualTo(MediaPlatform.YOUTUBE);
	}

	@Test
	void 구글드라이브_URL_을_알아본다() {
		assertThat(MediaPlatform.detect("https://drive.google.com/file/d/xyz/view"))
			.isEqualTo(MediaPlatform.GOOGLE_DRIVE);
	}

	@Test
	void 구글포토_URL_을_알아본다() {
		assertThat(MediaPlatform.detect("https://photos.google.com/share/xyz?key=abc"))
			.isEqualTo(MediaPlatform.GOOGLE_PHOTOS);
		assertThat(MediaPlatform.detect("https://photos.app.goo.gl/abc123")).isEqualTo(MediaPlatform.GOOGLE_PHOTOS);
	}

	@Test
	void 나머지는_OTHER() {
		assertThat(MediaPlatform.detect("https://vimeo.com/123")).isEqualTo(MediaPlatform.OTHER);
		assertThat(MediaPlatform.detect(null)).isEqualTo(MediaPlatform.OTHER);
	}

}
