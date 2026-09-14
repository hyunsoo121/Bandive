package com.bandive.bandive.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 컨트롤러 파라미터에서 현재 로그인 사용자 id 를 꺼낸다.
 *
 * <pre>{@code
 * &#64;PostMapping("/api/bands")
 * BandResponse create(@CurrentUser Long userId, ...) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "id")
public @interface CurrentUser {

}
