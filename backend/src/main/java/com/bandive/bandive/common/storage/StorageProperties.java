package com.bandive.bandive.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.storage.*}.
 *
 * @param localDir 로컬 디스크 저장 루트
 * @param publicBaseUrl 반환 URL 앞에 붙는 베이스 (로컬은 우리 서버, prod 는 S3/CDN)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(@DefaultValue("./uploads") String localDir,
		@DefaultValue("http://localhost:8081") String publicBaseUrl) {
}
