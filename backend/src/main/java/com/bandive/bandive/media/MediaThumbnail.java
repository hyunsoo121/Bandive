package com.bandive.bandive.media;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 외부 URL 에서 썸네일 이미지 주소를 뽑아낸다. 우리가 저장하지 않고 응답 시 계산해서 내려준다.
 * <ul>
 * <li>YouTube — 영상 id → {@code img.youtube.com/vi/{id}/hqdefault.jpg} (무인증)</li>
 * <li>Google Drive — 파일 id → {@code drive.google.com/thumbnail?id={id}&sz=w400} (공유 설정에 따라 실패할 수 있음)</li>
 * <li>그 외 — null</li>
 * </ul>
 */
public final class MediaThumbnail {

	private static final Pattern YOUTUBE_ID = Pattern
		.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/))([A-Za-z0-9_-]{11})");

	private static final Pattern DRIVE_ID = Pattern
		.compile("drive\\.google\\.com/(?:file/d/|open\\?(?:.*&)?id=|uc\\?(?:.*&)?id=)([A-Za-z0-9_-]{10,})");

	private MediaThumbnail() {
	}

	public static String of(MediaPlatform platform, String url) {
		if (url == null || platform == null) {
			return null;
		}
		return switch (platform) {
			case YOUTUBE -> youtube(url);
			case GOOGLE_DRIVE -> drive(url);
			case OTHER -> null;
		};
	}

	private static String youtube(String url) {
		Matcher m = YOUTUBE_ID.matcher(url);
		return m.find() ? "https://img.youtube.com/vi/" + m.group(1) + "/hqdefault.jpg" : null;
	}

	private static String drive(String url) {
		Matcher m = DRIVE_ID.matcher(url);
		return m.find() ? "https://drive.google.com/thumbnail?id=" + m.group(1) + "&sz=w400" : null;
	}

}
