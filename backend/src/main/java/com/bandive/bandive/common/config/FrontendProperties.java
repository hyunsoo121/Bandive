package com.bandive.bandive.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.frontend.*}. 초대 링크 등 사용자에게 노출할 프론트 URL 을 만들 때 쓴다.
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(@DefaultValue("http://localhost:5173") String baseUrl) {
}
