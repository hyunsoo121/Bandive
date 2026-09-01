# 트러블슈팅

Bandive 세팅/개발 중 실제로 겪은 문제와 해결책. 새 항목은 **증상 → 원인 → 해결** 순서로 추가.

---

## Phase 0 (부트스트랩)

### 포트 충돌 — `Bind for 0.0.0.0:6379 failed: port is already allocated`

**증상.** `docker compose up -d` 시 redis/postgres 컨테이너가 뜨지 않음.
`./gradlew bootRun` 은 `Web server failed to start. Port 8080 was already in use`.

**원인.** 같은 머신의 다른 프로젝트(PickYouth 등)가 이미 `5432`(Postgres) · `6379`(Redis) · `8080`(Spring)
을 점유. 확인:

```sh
docker ps --format '{{.Names}}\t{{.Ports}}'
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

**해결.** Bandive 는 기본 포트를 비켜서 씀:

| 서비스 | Bandive 포트 | 정의 위치 |
|---|---|---|
| Postgres | **5433** | `backend/docker-compose.yml`, `application.yaml` 의 `DB_URL` 기본값 |
| Redis | **6380** | `backend/docker-compose.yml`, `application.yaml` 의 `REDIS_PORT` 기본값 |
| 백엔드 HTTP | **8081** | `application.yaml` 의 `server.port` 기본값 |

환경변수(`DB_URL`, `REDIS_PORT`, `SERVER_PORT`)로 언제든 덮어쓸 수 있음. Testcontainers 통합 테스트는
랜덤 포트라 무관.

---

### Flyway 가 조용히 아무것도 안 함 (마이그레이션 미실행)

**증상.** `org.flywaydb:flyway-core` 를 추가했는데 부팅 로그에 Flyway 관련 줄이 전혀 없고
`flyway_schema_history` 테이블도 안 생김. 에러도 없음.

**원인.** **Spring Boot 4 는 auto-configuration 을 기술별 모듈로 분리**했다. Boot 3 에서는
`flyway-core` 만 있으면 `spring-boot-autoconfigure` 안의 `FlywayAutoConfiguration` 이 동작했지만,
Boot 4 에서는 그 클래스가 별도 모듈 `org.springframework.boot:spring-boot-flyway` 로 빠졌고
`flyway-core` 는 이 모듈을 끌어오지 않는다. 결과적으로 Flyway 빈 자체가 생성되지 않음.

**해결.** `build.gradle` 에서 `flyway-core` 대신 통합 모듈을 의존:

```groovy
implementation 'org.springframework.boot:spring-boot-flyway'      // flyway-core 를 전이 포함
implementation 'org.flywaydb:flyway-database-postgresql'
```

> 같은 패턴의 다른 모듈: `spring-boot-jdbc`, `spring-boot-jpa` 등. JPA/JDBC 는 `starter-data-jpa` 가
> 챙겨주지만 Flyway 는 스타터가 없어서 직접 넣어야 한다.

---

### Testcontainers — `Could not find org.testcontainers:junit-jupiter:.`

**증상.** `compileTestJava` 실패, 버전이 빈 채로 `org.testcontainers:junit-jupiter` /
`org.testcontainers:postgresql` 를 못 찾음.

**원인.** Spring Boot 4.1 이 관리하는 Testcontainers 가 **2.x** 인데, Testcontainers 2.0 에서
모든 모듈 아티팩트명에 `testcontainers-` 접두가 붙었다. 옛 이름은 BOM 에 없어 버전이 안 붙음.

**해결.** 새 아티팩트명 사용:

```groovy
testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
testImplementation 'org.testcontainers:testcontainers-postgresql'
```

---

### Testcontainers 2.x — `PostgreSQLContainer does not take parameters`

**증상.** `new PostgreSQLContainer<>("postgres:17-alpine")` 컴파일 에러
(`cannot use '<>' with non-generic class`). 또한 `org.testcontainers.containers.PostgreSQLContainer`
는 `@deprecated`.

**원인.** TC 2.x 에서 DB 컨테이너 클래스가 모듈별 패키지로 이동하고 제네릭(`SELF`)을 뗐다.
`org.testcontainers.containers.PostgreSQLContainer`(제네릭, deprecated)
→ `org.testcontainers.postgresql.PostgreSQLContainer`(비제네릭).

**해결.**

```java
import org.testcontainers.postgresql.PostgreSQLContainer;
// ...
static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine"); // <> 없이
```

`GenericContainer<SELF>` 는 2.x 에서도 제네릭 유지 → Redis 컨테이너는 `GenericContainer<?>` 그대로.

---

### `@SpringBootTest` contextLoads 가 DB 없이 실패

**증상.** Initializr 기본 `BandiveApplicationTests.contextLoads` 가 datasource 없이 실패.

**원인.** `ddl-auto=validate` + 실제 DB 필요. H2 같은 임베디드 DB 미포함.

**해결.** `support/IntegrationTest` 베이스 클래스가 Testcontainers 로 실제 Postgres/Redis 를 띄우고
`@ServiceConnection` 으로 접속 정보를 주입. 모든 통합 테스트는 이 클래스를 상속.
**Docker 데몬이 떠 있어야 `./gradlew test` 가 돈다.**

---

### pre-commit 훅이 안 걸림

**증상.** 커밋해도 lint/format 검사가 안 돎.

**원인.** `core.hooksPath` 미설정 (clone 직후) 또는 `hooks/pre-commit` 실행권한 없음.

**해결.** `sh hooks/install.sh` 한 번 실행. 훅을 우회해야 하면 `git commit --no-verify`.
