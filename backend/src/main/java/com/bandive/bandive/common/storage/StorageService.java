package com.bandive.bandive.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일 저장 추상화. 지금은 {@link LocalStorageService}(로컬 디스크), Phase 5 에서 S3 구현체로 교체.
 * 호출부(서비스)는 이 인터페이스만 의존한다.
 */
public interface StorageService {

	/**
	 * 파일을 저장하고 공개 접근 가능한 URL 을 돌려준다.
	 * @param directory 논리적 분류 (예: {@code "band-logo"})
	 * @param file 업로드된 파일
	 * @return 저장된 파일의 URL
	 */
	String store(String directory, MultipartFile file);

	/** 더 이상 쓰지 않는 파일 정리. 없는 URL 이면 조용히 무시한다. */
	void delete(String url);

}
