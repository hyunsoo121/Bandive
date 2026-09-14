package com.bandive.bandive.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.bandive.bandive.common.config.JpaConfig;

/**
 * 슬라이스(JPA) + 실제 Postgres + Flyway V1 로 도는 Repository 테스트 베이스. 임베디드 DB 치환을 끄고
 * {@link AbstractContainerTest} 의 싱글턴 컨테이너를 쓴다. {@code @DataJpaTest} 슬라이스는
 * {@code @Configuration} 을 스캔하지 않으므로 Auditing 설정을 직접 import 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositoryTest extends AbstractContainerTest {

}
