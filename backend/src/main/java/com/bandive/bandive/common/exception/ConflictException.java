package com.bandive.bandive.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 현재 상태와 충돌하는 요청 (409). 예: 곡당 1표 제약 위반(중복 투표), 이미 가입한 밴드에 재가입.
 */
public class ConflictException extends BandiveException {

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, "CONFLICT", message);
	}

	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}

}
