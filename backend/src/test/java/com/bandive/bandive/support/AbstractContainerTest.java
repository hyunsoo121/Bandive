package com.bandive.bandive.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * JVM 당 한 번 뜨는 싱글턴 Postgres/Redis 컨테이너. {@code @SpringBootTest}(→
 * {@link IntegrationTest})와 {@code @DataJpaTest}(→ {@link RepositoryTest})가 같은 컨테이너를
 * 공유한다. Docker 데몬이 필요하다.
 */
public abstract class AbstractContainerTest {

	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

}
