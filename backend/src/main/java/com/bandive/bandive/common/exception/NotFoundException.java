package com.bandive.bandive.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스가 없음 (404). 도메인별로 {@code code} 를 구체화한다:
 * {@code new NotFoundException("BAND_NOT_FOUND", "밴드를 찾을 수 없습니다.")}.
 */
public class NotFoundException extends BandiveException {

	public NotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
	}

	public NotFoundException(String code, String message) {
		super(HttpStatus.NOT_FOUND, code, message);
	}

}
