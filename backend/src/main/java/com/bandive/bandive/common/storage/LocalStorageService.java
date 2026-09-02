package com.bandive.bandive.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.bandive.bandive.common.exception.ValidationException;

/**
 * 로컬 디스크 구현. 파일은 {@code {localDir}/{directory}/{uuid}.{ext}} 로 저장하고
 * {@code {publicBaseUrl}/files/{directory}/{uuid}.{ext}} 를 반환한다. 이 URL 은
 * {@code WebMvcConfig} 의 리소스 핸들러가 서빙한다.
 */
@Service
@EnableConfigurationProperties(StorageProperties.class)
public class LocalStorageService implements StorageService {

	public static final String URL_PREFIX = "/files/";

	private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of("image/png", "png", "image/jpeg", "jpg",
			"image/webp", "webp", "image/gif", "gif");

	private final Path root;

	private final String publicBaseUrl;

	public LocalStorageService(StorageProperties properties) {
		this.root = Path.of(properties.localDir()).toAbsolutePath().normalize();
		this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
	}

	@PostConstruct
	void ensureRoot() {
		try {
			Files.createDirectories(root);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("업로드 디렉토리 생성 실패: " + root, ex);
		}
	}

	@Override
	public String store(String directory, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ValidationException("FILE_REQUIRED", "업로드할 파일이 없습니다.");
		}
		String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
		if (extension == null) {
			throw new ValidationException("UNSUPPORTED_FILE_TYPE", "이미지 파일(png, jpg, webp, gif)만 올릴 수 있습니다.");
		}

		String filename = UUID.randomUUID() + "." + extension;
		Path dir = root.resolve(directory).normalize();
		if (!dir.startsWith(root)) {
			throw new ValidationException("INVALID_PATH", "잘못된 경로입니다.");
		}
		try {
			Files.createDirectories(dir);
			file.transferTo(dir.resolve(filename));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("파일 저장 실패", ex);
		}
		return publicBaseUrl + URL_PREFIX + directory + "/" + filename;
	}

	@Override
	public void delete(String url) {
		if (!StringUtils.hasText(url)) {
			return;
		}
		int idx = url.indexOf(URL_PREFIX);
		if (idx < 0) {
			return;
		}
		Path target = root.resolve(url.substring(idx + URL_PREFIX.length())).normalize();
		if (target.startsWith(root)) {
			try {
				Files.deleteIfExists(target);
			}
			catch (IOException ignored) {
				// 정리는 베스트 에포트
			}
		}
	}

	private static String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

}
