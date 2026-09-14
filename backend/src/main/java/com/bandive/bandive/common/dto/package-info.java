/**
 * DTO 컨벤션 (도메인 패키지의 {@code dto/} 하위에 둔다. 이 패키지는 공용 DTO 전용).
 *
 * <ul>
 * <li><b>요청</b> {@code XxxRequest} — {@code record}. 형식 검증은 필드에 Jakarta Validation
 * ({@code @NotBlank}, {@code @Size}, {@code @Positive} …). 컨트롤러 파라미터에 {@code @Valid}.
 * 메시지는 사용자에게 그대로 노출되므로 한국어 한 문장으로 (예: {@code @NotBlank(message = "제목은 필수입니다")}).</li>
 * <li><b>응답</b> {@code XxxResponse} — {@code record}.
 * {@code static XxxResponse from(Entity e)} (목록은
 * {@code static List<XxxResponse> listOf(List<Entity>)}) 팩토리로 변환한다.</li>
 * <li>엔티티를 컨트롤러 시그니처(파라미터/반환)에 직접 노출하지 않는다. 지연로딩·과다응답·필드 유출 방지.</li>
 * <li>성공 응답은 DTO 를 그대로 반환한다. 공통 래퍼는 쓰지 않는다. 에러만
 * {@link com.bandive.bandive.common.response.ErrorResponse} 스키마로 통일된다.</li>
 * <li>도메인 규칙 위반은 {@link com.bandive.bandive.common.exception.BandiveException} 계열을 던진다
 * ({@code NotFoundException} / {@code ForbiddenException} / {@code ConflictException} /
 * {@code ValidationException}).</li>
 * </ul>
 */
package com.bandive.bandive.common.dto;
