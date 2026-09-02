package com.bandive.bandive.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션이 의도적으로 던지는 예외의 최상위. HTTP 상태와 기계가 분기할 수 있는 {@code code} 를 함께 들고 다닌다.
 * {@code message} 는 그대로 사용자에게 노출되므로 짧고 이해 가능한 설명으로 쓴다.
 *
 * <p>
 * 흔한 경우는 {@link NotFoundException} / {@link ForbiddenException} /
 * {@link ConflictException} / {@link ValidationException} 을 쓰고, 그 밖의 상태코드가 필요하면 이 클래스를 직접
 * 던진다.
 */
public class BandiveException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	public BandiveException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

}
