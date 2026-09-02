package com.bandive.bandive.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증은 됐으나 이 동작을 할 권한이 없음 (403). 예: 밴드장만 가능한 작업을 일반 멤버가 시도.
 */
public class ForbiddenException extends BandiveException {

	public ForbiddenException(String message) {
		super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
	}

	public ForbiddenException(String code, String message) {
		super(HttpStatus.FORBIDDEN, code, message);
	}

}
