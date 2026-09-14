package com.bandive.bandive.common.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bandive.bandive.common.storage.LocalStorageService;
import com.bandive.bandive.common.storage.StorageProperties;

/**
 * 로컬 디스크에 저장한 업로드 파일을 {@code /files/**} 로 서빙한다. Phase 5 에서 S3 로 옮기면 이 핸들러는 제거.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

	private final StorageProperties storageProperties;

	public WebMvcConfig(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path root = Path.of(storageProperties.localDir()).toAbsolutePath().normalize();
		registry.addResourceHandler(LocalStorageService.URL_PREFIX + "**")
			.addResourceLocations(root.toUri().toString());
	}

}
