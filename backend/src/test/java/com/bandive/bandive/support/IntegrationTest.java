package com.bandive.bandive.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 전체 스프링 컨텍스트 + 실제 Postgres/Redis 통합 테스트 베이스.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTest extends AbstractContainerTest {

}
