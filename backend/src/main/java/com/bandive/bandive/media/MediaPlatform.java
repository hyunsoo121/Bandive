package com.bandive.bandive.media;

/**
 * 영상 호스팅 플랫폼. 등록 시 external_url 로 추론한다.
 */
public enum MediaPlatform {

	YOUTUBE, GOOGLE_DRIVE, OTHER;

	public static MediaPlatform detect(String url) {
		if (url == null) {
			return OTHER;
		}
		String lower = url.toLowerCase();
		if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
			return YOUTUBE;
		}
		if (lower.contains("drive.google.com") || lower.contains("docs.google.com")) {
			return GOOGLE_DRIVE;
		}
		return OTHER;
	}

}
