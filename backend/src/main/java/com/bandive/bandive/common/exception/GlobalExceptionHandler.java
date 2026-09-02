package com.bandive.bandive.common.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bandive.bandive.common.response.ErrorResponse;

/**
 * 모든 컨트롤러 예외를 {@link ErrorResponse} 하나로 통일한다. 성공 응답은 손대지 않는다. 필드 단위 상세 덤프 대신 "왜 실패했는지" 한
 * 문장으로 준다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 우리가 의도적으로 던진 예외. */
	@ExceptionHandler(BandiveException.class)
	public ResponseEntity<ErrorResponse> handleBandive(BandiveException ex, HttpServletRequest request) {
		if (ex.getStatus().is5xxServerError()) {
			log.error("BandiveException 5xx at {}", request.getRequestURI(), ex);
		}
		else {
			log.warn("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), ex.getStatus().value(),
					ex.getMessage());
		}
		return ResponseEntity.status(ex.getStatus())
			.body(ErrorResponse.of(ex.getStatus(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
	}

	/** 메서드 보안(@PreAuthorize) 거부. */
	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex,
			HttpServletRequest request) {
		log.warn("접근 거부 {} {}", request.getMethod(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ErrorResponse.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 작업을 수행할 권한이 없습니다.", request.getRequestURI()));
	}

	/** 예상 못한 모든 예외 — 내부 정보는 로그로만, 응답은 일반 메시지. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("처리되지 않은 예외 at {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "요청을 처리하는 중 오류가 발생했습니다.",
					request.getRequestURI()));
	}

	/** Bean Validation (@Valid) 실패 — 첫 위반 사유를 사람이 읽을 수 있는 문장으로. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getDefaultMessage())
			.filter(text -> text != null && !text.isBlank())
			.findFirst()
			.orElse("입력값이 올바르지 않습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, path(request)));
	}

	/** 그 밖의 프레임워크 예외(405, 깨진 JSON, 누락 파라미터, 핸들러 없음 등)도 같은 스키마로. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ErrorResponse error = ErrorResponse.of(statusCode, codeOf(statusCode), messageOf(statusCode), path(request));
		return ResponseEntity.status(statusCode).headers(headers).body(error);
	}

	private static String codeOf(HttpStatusCode statusCode) {
		HttpStatus resolved = HttpStatus.resolve(statusCode.value());
		return resolved != null ? resolved.name() : "ERROR";
	}

	private static String messageOf(HttpStatusCode statusCode) {
		return switch (statusCode.value()) {
			case 400 -> "요청 형식이 올바르지 않습니다.";
			case 404 -> "요청한 리소스를 찾을 수 없습니다.";
			case 405 -> "허용되지 않은 요청 방식입니다.";
			case 415 -> "지원하지 않는 요청 형식입니다.";
			default -> "요청을 처리할 수 없습니다.";
		};
	}

	private static String path(WebRequest request) {
		return request instanceof ServletWebRequest servletRequest ? servletRequest.getRequest().getRequestURI() : "";
	}

}
