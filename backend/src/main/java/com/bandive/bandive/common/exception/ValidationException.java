package com.bandive.bandive.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 입력값이 규칙에 맞지 않음 (400). Bean Validation 으로 못 잡는 도메인 규칙 위반에 쓴다 (예: "확정곡이 아니면 파트를 배정할 수
 * 없습니다"). 형식 검증 실패는 {@code MethodArgumentNotValidException} 으로 자동 처리된다.
 */
public class ValidationException extends BandiveException {

	public ValidationException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}

	public ValidationException(String code, String message) {
		super(HttpStatus.BAD_REQUEST, code, message);
	}

}
