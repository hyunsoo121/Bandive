package com.bandive.bandive.common.response;

import java.time.Instant;

import org.springframework.http.HttpStatusCode;

/**
 * 모든 에러 응답의 공통 스키마. 성공 응답은 각 DTO 를 그대로 반환하고 래핑하지 않는다.
 *
 * <pre>
 * { "status": 404, "code": "BAND_NOT_FOUND", "message": "밴드를 찾을 수 없습니다.",
 *   "path": "/api/bands/999", "timestamp": "2026-09-02T04:12:00Z" }
 * </pre>
 */
public record ErrorResponse(int status, String code, String message, String path, Instant timestamp) {

	public static ErrorResponse of(HttpStatusCode status, String code, String message, String path) {
		return new ErrorResponse(status.value(), code, message, path, Instant.now());
	}

}
