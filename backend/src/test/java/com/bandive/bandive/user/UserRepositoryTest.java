package com.bandive.bandive.user;

import org.junit.jupiter.api.Test;

import com.bandive.bandive.support.Fixtures;
import com.bandive.bandive.support.RepositoryTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends RepositoryTest {

	@Autowired
	private UserRepository users;

	@Autowired
	private TestEntityManager em;

	@Test
	void 저장하면_생성시각이_자동으로_채워진다() {
		User saved = users.save(Fixtures.user("kakao-1"));
		em.flush();

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	void kakaoId_로_조회한다() {
		users.save(Fixtures.user("kakao-2"));
		em.flush();
		em.clear();

		assertThat(users.findByKakaoId("kakao-2")).isPresent();
		assertThat(users.findByKakaoId("none")).isEmpty();
	}

	@Test
	void kakaoId_는_중복될_수_없다() {
		users.save(Fixtures.user("dup"));
		em.flush();

		assertThatThrownBy(() -> {
			users.save(Fixtures.user("dup"));
			em.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
