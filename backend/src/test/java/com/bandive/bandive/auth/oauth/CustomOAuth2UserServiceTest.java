package com.bandive.bandive.auth.oauth;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.support.RepositoryTest;
import com.bandive.bandive.user.User;
import com.bandive.bandive.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2UserServiceTest extends RepositoryTest {

	@Autowired
	private UserRepository users;

	private CustomOAuth2UserService service() {
		return new CustomOAuth2UserService(users);
	}

	@Test
	void 처음_로그인이면_kakao_id_와_닉네임으로_유저를_만든다() {
		Map<String, Object> attributes = Map.of("id", 987654321L, "kakao_account",
				Map.of("profile", Map.of("nickname", "홍길동")));

		User provisioned = service().provision(attributes);

		assertThat(provisioned.getId()).isNotNull();
		assertThat(provisioned.getKakaoId()).isEqualTo("987654321");
		assertThat(provisioned.getNickname()).isEqualTo("홍길동");
	}

	@Test
	void 이미_있으면_같은_유저를_재사용한다() {
		Map<String, Object> attributes = Map.of("id", 111L, "kakao_account",
				Map.of("profile", Map.of("nickname", "기존")));
		Long firstId = service().provision(attributes).getId();

		Long secondId = service().provision(attributes).getId();

		assertThat(secondId).isEqualTo(firstId);
		assertThat(users.findByKakaoId("111")).isPresent();
	}

	@Test
	void 닉네임_동의가_없으면_fallback_닉네임을_쓴다() {
		User provisioned = service().provision(Map.of("id", 222L));

		assertThat(provisioned.getNickname()).isEqualTo("user222");
	}

}
