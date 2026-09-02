package com.bandive.bandive.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bandive.bandive.auth.AuthProperties;
import com.bandive.bandive.auth.jwt.JwtAuthenticationFilter;
import com.bandive.bandive.auth.oauth.CustomOAuth2UserService;
import com.bandive.bandive.auth.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.bandive.bandive.auth.oauth.OAuth2LoginFailureHandler;
import com.bandive.bandive.auth.oauth.OAuth2LoginSuccessHandler;
import com.bandive.bandive.common.security.RestAccessDeniedHandler;
import com.bandive.bandive.common.security.RestAuthenticationEntryPoint;

/**
 * 인증: 카카오 oauth2Login (세션 대신 쿠키에 authorization request). 인가: 공개 GET / 그 외 인증. API 는
 * STATELESS + Bearer JWT. 밴드장 전용은 메서드 단위 {@code @PreAuthorize}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
			CustomOAuth2UserService customOAuth2UserService, OAuth2LoginSuccessHandler successHandler,
			OAuth2LoginFailureHandler failureHandler,
			HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
			RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler)
			throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/info", "/error")
				.permitAll()
				.requestMatchers("/oauth2/**", "/login/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/refresh", "/api/auth/logout")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/me")
				.authenticated()
				.requestMatchers(HttpMethod.GET, "/api/**")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2Login(oauth -> oauth
				.authorizationEndpoint(
						endpoint -> endpoint.authorizationRequestRepository(authorizationRequestRepository))
				.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				.successHandler(successHandler)
				.failureHandler(failureHandler))
			.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}
