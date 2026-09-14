package com.bandive.bandive.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인에 묶이지 않는 잡다한 {@code @ConfigurationProperties} 등록 지점.
 */
@Configuration
@EnableConfigurationProperties(FrontendProperties.class)
public class AppConfig {

}
