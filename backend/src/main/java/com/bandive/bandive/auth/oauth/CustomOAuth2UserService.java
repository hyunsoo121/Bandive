package com.bandive.bandive.auth.oauth;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

/**
 * 카카오 user-info 응답으로 우리 {@link User} 를 upsert 한다 (kakao_id 기준). 이메일은 동의항목에서 제외했으므로 다루지
 * 않는다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository users;

	public CustomOAuth2UserService(UserRepository users) {
		this.users = users;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User kakaoUser = super.loadUser(userRequest);
		User user = provision(kakaoUser.getAttributes());
		return new KakaoOAuth2User(user.getId(), kakaoUser.getAttributes());
	}

	/**
	 * 카카오 attributes ({@code id}, {@code kakao_account.profile.nickname}) 로 User 를 찾거나
	 * 만든다. 테스트에서 실제 카카오 호출 없이 검증할 수 있도록 분리해 둔다.
	 */
	@Transactional
	User provision(Map<String, Object> attributes) {
		String kakaoId = String.valueOf(attributes.get("id"));
		String nickname = extractNickname(attributes, kakaoId);
		return users.findByKakaoId(kakaoId)
			.orElseGet(() -> users.save(User.builder().kakaoId(kakaoId).nickname(nickname).build()));
	}

	@SuppressWarnings("unchecked")
	private String extractNickname(Map<String, Object> attributes, String kakaoId) {
		Object account = attributes.get("kakao_account");
		if (account instanceof Map<?, ?> accountMap) {
			Object profile = ((Map<String, Object>) accountMap).get("profile");
			if (profile instanceof Map<?, ?> profileMap) {
				Object nickname = ((Map<String, Object>) profileMap).get("nickname");
				if (nickname instanceof String s && !s.isBlank()) {
					return s;
				}
			}
		}
		return "user" + kakaoId;
	}

}
