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

**해결.** `support/AbstractContainerTest` 가 JVM 당 1회 뜨는 싱글턴 Postgres/Redis 컨테이너 +
`@DynamicPropertySource` 로 접속 정보를 주입. `IntegrationTest`(`@SpringBootTest`)와
`RepositoryTest`(`@DataJpaTest`)가 이걸 상속해 같은 컨테이너를 공유한다.
**Docker 데몬이 떠 있어야 `./gradlew test` 가 돈다.**

---

## Phase 1 (도메인 모델)

### `@DataJpaTest` 에서 `createdAt` 이 null → NOT NULL 제약 위반

**증상.** Repository 테스트에서 엔티티 저장 시
`null value in column "created_at" ... violates not-null constraint`.

**원인.** `@EnableJpaAuditing` 을 `common/config/JpaConfig`(`@Configuration`)에 뒀는데,
`@DataJpaTest` **슬라이스는 `@Configuration` 을 컴포넌트 스캔하지 않는다.** 그래서 Auditing 리스너가
컨텍스트에 없고 `@CreatedDate`/`@LastModifiedDate` 가 안 채워짐. (`@SpringBootTest` 는 전체 스캔이라 정상)

**해결.** `RepositoryTest` 베이스에 `@Import(JpaConfig.class)` 추가.

---

### Flyway `checksum mismatch` — 이미 적용된 마이그레이션을 수정함

**증상.** 개발 중 `V1__init.sql` 을 고치고 앱을 다시 띄우면
`Migration checksum mismatch for migration version 1`.

**원인.** Flyway 는 적용 완료된 마이그레이션 파일의 해시를 `flyway_schema_history` 에 저장하고,
이후 파일이 바뀌면 무결성 위반으로 본다. **아직 배포 전이라 로컬에서 V1 을 계속 편집하는 상황**에서 발생.

**해결 (로컬, 데이터 버려도 될 때).** 볼륨째 초기화:

```sh
cd backend && docker compose down -v && docker compose up -d
```

배포 후에는 절대 적용된 마이그레이션을 수정하지 말 것 — 항상 `V2__*.sql` 를 새로 추가.

---

### Boot 4 — 테스트 슬라이스 애너테이션 패키지 이동

**증상.** `import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;` 등이
컴파일 안 됨 (클래스 없음).

**원인.** Boot 4 가 테스트 자동설정도 모듈별로 쪼갬. 새 좌표:

| 애너테이션 | Boot 4 패키지 | 제공 모듈 |
|---|---|---|
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure` | `spring-boot-data-jpa-test` |
| `@AutoConfigureTestDatabase` | `org.springframework.boot.jdbc.test.autoconfigure` | `spring-boot-jdbc-test` |
| `TestEntityManager` | `org.springframework.boot.jpa.test.autoconfigure` | `spring-boot-jpa-test` |

모듈 자체는 `spring-boot-starter-data-jpa-test` 가 다 끌어온다. import 경로만 갱신하면 됨.
`@AutoConfigureMockMvc` 도 마찬가지 — `org.springframework.boot.webmvc.test.autoconfigure`.

---

## Phase 2 (카카오 OAuth2 + JWT)

### `.env` 를 어떻게 읽나

`application.yaml` 에 `spring.config.import: optional:file:.env[.properties]` 를 넣었다.
`backend/.env` 를 properties 형식(`KEY=value`)으로 읽어 `${KAKAO_CLIENT_ID}` 같은 placeholder 를 채운다.
`optional:` 이라 파일이 없어도(=CI) 기동은 되고, `[.properties]` 는 확장자와 무관하게 properties 로 파싱하라는 힌트.
`bootRun` / 테스트 모두 작업 디렉토리가 `backend/` 라 상대경로가 맞는다.

### 기동 실패: `localhost:5433 에 대한 연결이 거부되었습니다` (Flyway/Hikari)

**원인.** docker-compose 의 Postgres/Redis 컨테이너가 안 떠 있음. `bootRun` 전에 인프라를 먼저 올려야 한다.

**해결.** `cd backend && docker compose up -d` (healthy 확인) 후 `./gradlew bootRun`.
컨테이너 상태는 `docker compose ps`.

---

### 기동 실패: `app.auth.jwt.secret 이 설정되지 않았거나 32바이트 미만입니다`

**원인.** `backend/.env` 에 `JWT_SECRET`(또는 `KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET`)이 없음.
`@ConfigurationProperties` 바인딩은 미해결 placeholder 를 리터럴 `"${JWT_SECRET}"` 로 남기므로
(환경 프로퍼티 resolver 가 `ignoreUnresolvableNestedPlaceholders=true`), `JwtProvider` 생성자에서 걸린다.

**해결.** `backend/.env.example` 을 참고해 `backend/.env` 에 3개 키를 채운다:
`KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `JWT_SECRET`(≥32바이트, `openssl rand -base64 48`).
테스트는 `application-test.yaml` 의 더미값을 쓰므로 `.env` 없이도 통과한다.

### 카카오 로그인 시 `KOE101` (invalid client) / redirect 후 에러

**원인.** `KAKAO_CLIENT_ID` 미설정(리터럴 `${...}` 전송) 또는 카카오 콘솔에
**Redirect URI 미등록**. 우리 기본 콜백은 `http://localhost:8081/login/oauth2/code/kakao`.

**해결.** `.env` 값 확인 + 카카오 디벨로퍼스 > 카카오 로그인 > Redirect URI 에 위 주소 등록.

### `permitAll` 로 열어둔 GET 인데 401 이 나온다

**증상.** `GET /api/bands/1` 이 (컨트롤러가 아직 없어) 404 대신 401.

**원인.** 핸들러 없는 요청은 서블릿 컨테이너가 `/error` 로 forward 하는데, `/error` 가 인가 규칙에
안 걸려 있으면 `anyRequest().authenticated()` 에 잡혀 401 이 된다. (MockMvc 는 이 forward 를 안 타서
테스트에선 404 로 보여 놓치기 쉽다 — 반드시 실제 기동으로 확인.)

**해결.** `SecurityConfig` 인가 규칙에 `"/error"` 를 `permitAll` 로 추가.

---

## Phase 3 (공통 인프라)

### `package com.fasterxml.jackson.databind does not exist`

**원인.** Spring Boot 4 / Spring Framework 7 은 **Jackson 3** 을 쓴다. 3.x 에서 패키지 루트가
`com.fasterxml.jackson.*` → **`tools.jackson.*`** 로 바뀌었다 (`jackson-databind` 좌표도
`tools.jackson.core:jackson-databind`).

**해결.** import 를 `tools.jackson.databind.ObjectMapper` 로. Jackson 3 에서 직렬화 예외는
unchecked (`tools.jackson.core.JacksonException extends RuntimeException`) 라 `throws IOException`
없어도 된다. Boot 이 만들어주는 `ObjectMapper`(사실상 `tools.jackson.databind.json.JsonMapper`) 빈은
그대로 주입된다.

### 에러 응답이 두 종류로 갈린다 (필터 vs 컨트롤러)

**증상.** 컨트롤러에서 던진 예외는 `ErrorResponse` 스키마인데, 인증 실패(401)·인가 실패(403)는
Spring Security 기본 응답.

**원인.** `@RestControllerAdvice` 는 **DispatcherServlet 안**에서만 동작. 인증/인가는 그 앞
**필터 단계**라 advice 를 안 탄다.

**해결.** `RestAuthenticationEntryPoint`(401) / `RestAccessDeniedHandler`(403) 에서 `ObjectMapper` 로
같은 `ErrorResponse` 를 직접 써준다. `@PreAuthorize`(메서드 보안) 거부는 컨트롤러 이후라
`GlobalExceptionHandler` 의 `AuthorizationDeniedException` 핸들러가 잡는다.

---

## Phase 4 (도메인 API)

### `@WebMvcTest` 컨텍스트 로드 실패 — `No qualifying bean of type 'JwtProvider'`

**원인.** `@WebMvcTest` 슬라이스는 컨트롤러뿐 아니라 **`Filter` / `WebMvcConfigurer` / `@ControllerAdvice` 빈도
같이 올린다.** `JwtAuthenticationFilter`(`@Component`, `OncePerRequestFilter`)가 딸려 올라오면서
생성자 의존 `JwtProvider` 를 찾다 실패.

**해결.** 그 의존을 `@MockitoBean` 으로 채운다:

```java
@WebMvcTest(BandController.class)
class BandControllerTest {
    @MockitoBean BandService bandService;
    @MockitoBean(name = "bandGuard") BandGuard bandGuard;   // @PreAuthorize SpEL 용
    @MockitoBean JwtProvider jwtProvider;                    // 딸려온 JwtAuthenticationFilter 용
```

`@PreAuthorize` 를 태우려면 슬라이스에 method security 가 필요하다 — 테스트 안 nested
`@TestConfiguration @EnableMethodSecurity` + permit-all `SecurityFilterChain` 하나 두면 된다.
현재 로그인 사용자는 `SecurityMockMvcRequestPostProcessors.authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id), ...))` 로 주입 (`@CurrentUser` 가 `principal.id` 를 읽음).

### 앱 기동 실패 — `Ambiguous @ExceptionHandler method mapped for [MaxUploadSizeExceededException]`

**원인.** `GlobalExceptionHandler extends ResponseEntityExceptionHandler`. 최근 Spring 은
`MaxUploadSizeExceededException`(그리고 `ErrorResponseException`, `MissingServletRequestPartException` 등)을
**base class 가 이미 `@ExceptionHandler` 로 처리**한다. 하위에서 같은 타입을 다시 선언하면 컨텍스트 초기화 때 터진다.

**해결.** base class 가 잡는 타입은 다시 선언하지 말고, 메시지/코드만 바꾸고 싶으면
`handleExceptionInternal` 오버라이드 안에서 `statusCode` 로 분기한다 (예: `case 413 -> "파일이 너무 큽니다"`).

### `.env` 값에 앞 공백이 있으면 로컬에선 되는데 다른 데선 깨진다

`JWT_SECRET= abc...` 처럼 `=` 뒤에 공백을 넣으면 **Spring properties 파서는 앞뒤 공백을 잘라내서** 앱은 정상 동작하지만,
쉘 `export` / docker env 로 같은 `.env` 를 읽으면 공백이 값에 포함돼 서명 불일치(401) 등이 난다. `.env` 값은 공백 없이 붙여 쓴다.

### WARN `... cannot get proxied via CGLIB` (CustomOAuth2UserService)

`CustomOAuth2UserService` 가 `DefaultOAuth2UserService` 를 상속하면서 `@Transactional` 메서드를 가져
CGLIB 프록시가 생기는데 부모의 `final` setter 를 못 감싼다는 **경고일 뿐**(우리는 그 setter 를 안 씀). 무시 가능.
거슬리면 upsert 로직을 별도 `@Service` 로 빼면 사라진다.

### `@DataJpaTest` 삭제 테스트 — `TransientPropertyValueException: ... references an unsaved transient instance`

**증상.** 서비스가 `bandRepository.delete(band)` 로 지우고 하위(band_members 등)는 DB `ON DELETE CASCADE` 에
맡기는데, `@DataJpaTest` 에서 그 서비스를 호출하면 `em.flush()` 시점에
`BandMember references an unsaved transient instance of Band`.

**원인.** `@DataJpaTest` 는 한 테스트 = 한 트랜잭션/세션. setup 에서 `service.create(...)` 로 만든
`BandMember` 가 영속성 컨텍스트에 **관리 상태로 남아** 있는데, 이후 `bands.delete(band)` 로 Band 가
삭제 예약되면 그 BandMember 의 `band` 참조가 transient 로 취급된다. (운영에선 삭제가 새 트랜잭션/세션에서
돌기 때문에 이 문제 없음 — `@PreAuthorize` 의 `BandGuard` 조회도 별도 짧은 트랜잭션.)

**해결.** 테스트에서 `service.delete(...)` 호출 직전에 `em.flush(); em.clear();` 로 컨텍스트를 비운다
(운영 상황을 흉내). 서비스 코드는 그대로 — DB cascade 방식 유지.

## Phase 4-4 (곡)

### 비회원도 되는 GET 에 `@CurrentUser` 를 쓰면 500

**증상.** `GET /api/bands/{bandId}/songs` (공개, `votedByMe` 계산용으로 현재 유저가 필요) 를 토큰 없이
부르면 500.

**원인.** `@CurrentUser` = `@AuthenticationPrincipal(expression = "id")`. 익명 요청이면
`AnonymousAuthenticationFilter` 가 principal 을 문자열 `"anonymousUser"` 로 채워두는데,
거기서 SpEL `id` 를 평가하다 터진다. (인증 강제된 엔드포인트는 principal 이 항상 `UserPrincipal` 이라 문제없음.)

**해결.** 선택적 인증 엔드포인트는 `@CurrentUser Long` 대신
`@AuthenticationPrincipal UserPrincipal principal` 를 받고 `principal != null ? principal.getId() : null`.
(`expression` 없는 `@AuthenticationPrincipal` 은 타입 불일치 시 기본값 null 을 준다.) Phase 4-5/4-6 의
공개 목록에서도 같은 패턴.

---

### pre-commit 훅이 안 걸림

**증상.** 커밋해도 lint/format 검사가 안 돎.

**원인.** `core.hooksPath` 미설정 (clone 직후) 또는 `hooks/pre-commit` 실행권한 없음.

**해결.** `sh hooks/install.sh` 한 번 실행. 훅을 우회해야 하면 `git commit --no-verify`.

---

## 프론트 ↔ 백엔드 연동 (frontend/src/api)

### 로그인은 되는데 새로고침하면 로그아웃된다

**증상.** 카카오 로그인 직후엔 멀쩡한데 F5 누르면 게스트로 돌아감.

**원인.** access 토큰은 메모리에만 둔다(그게 의도). 새로고침 시엔 refresh 쿠키로 다시 받아야 하는데,
`POST /api/auth/refresh` 가 401 이면 복구 실패.

**해결.** refresh 쿠키(`refresh_token`, httpOnly, path `/api/auth`)가 브라우저에 남아 있는지 확인.
- 백엔드가 `application-local.yaml` 로 떠서 `app.auth.cookie.secure=false` 여야 http localhost 에서 쿠키가 심긴다.
- 프론트 fetch 는 전부 `credentials: 'include'` (client.ts) — 빠지면 쿠키가 안 실린다.
- 로컬은 `localhost:5173` ↔ `localhost:8081` 로 **포트만 다르고 같은 site** 라 `SameSite=Strict` 쿠키도 전송된다.
  (배포 때 프론트/백엔드가 다른 도메인이면 `SameSite=None; Secure` 로 바꿔야 함 — Phase 7 숙제)

### CORS 에러 — `blocked by CORS policy` 또는 프리플라이트 실패

**원인.** 백엔드 `app.cors.allowed-origins` 에 프론트 오리진이 없거나, credentials 요청인데
와일드카드 오리진이라 브라우저가 거부.

**해결.** `backend/.env` (또는 기본값)에 `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173`.
`CorsConfig` 는 이미 `allowCredentials(true)` + 명시 오리진 + 모든 헤더/메서드 허용.
프론트 `VITE_API_ORIGIN` 이 그 오리진에서 백엔드를 가리키는지도 확인 (기본 `http://localhost:8081`).

### 초대 코드가 멤버 화면에 안 보인다

**증상.** 밴드장인데 멤버 탭에 초대 코드가 없음.

**원인.** 버그 아님. 초대 코드 **조회** API 가 없다 (`POST` 발급/재발급만 있음). 발급을 눌러야 표시된다.
`POST` 를 다시 부르면 이전 코드는 폐기되므로 자동 호출하지 않는 것.

### 밴드장인데 밴드 전환 시트에 "사용자" 로 표시된다

**원인.** `GET /api/bands/my` 응답에 아직 role 필드가 없다. 현재 밴드 화면의 권한 판정은
멤버 목록에서 내 역할을 찾아 정확히 하지만, 내 밴드 **목록**(스위처)에는 역할 정보가 없어 전부 'member' fallback.

**해결.** 백엔드가 my 응답에 role 을 실어주면 자동 해결 (`mappers.toBand` 가 이미 `dto.role` 을 읽음).

### 곡 "검색" 탭에 무슨 단어를 쳐도 결과 3건이 똑같이 나온다

**원인.** `GET /api/songs/search` 가 스텁 모드다. `app.music.provider` 기본값이 `stub`
(`StubMusicSearchService` — 쿼리를 3건으로 echo). 실 검색을 켜려면 `backend/.env` 에 **한 줄** `MUSIC_PROVIDER=itunes`
(Apple iTunes Search API — 인증·API 키 불필요. 선택: `MUSIC_COUNTRY=KR`). 응답 형태(`TrackSearchResult`)는
두 모드가 동일해서 프론트는 그대로 동작한다. 외부 장애(타임아웃·4xx·5xx·깨진 본문)는 `ItunesMusicSearchService` 가
삼켜서 빈 목록으로 준다(500 대신 결과 없음).
결과를 **클릭해서 골라야** `sourceType=SEARCH` +
`externalTrackId` 로 등록되고, 안 고르고 "직접 입력" 값만 쓰면 `MANUAL` 로 등록된다
(백엔드는 SEARCH 인데 `externalTrackId` 가 비면 `400 EXTERNAL_TRACK_ID_REQUIRED`).

### 왜 Spotify 가 아니라 iTunes Search API 인가

**증상.** Spotify 로 시도했더니 토큰은 받아지는데 `/v1/search` 가
`403 Forbidden: "Active premium subscription required for the owner of the app."` (2026-09-03 확인, 2회).

**원인.** Spotify Web API 는 `/v1/search` 등 대부분의 데이터 엔드포인트를 **앱(Client ID) 소유자의 Spotify 계정이
Premium** 일 때만 200 으로 준다. 무료 계정이 만든 앱은 토큰만 나오고 403.

**결정.** 유료 구독이 필요 없는 **Apple iTunes Search API** 채택. 인증·키·계정 전부 불필요, 영어·한국어 모두 잘 나옴.
`MUSIC_PROVIDER=itunes` 한 줄. iTunes 는 `Content-Type: text/javascript` 로 JSON 을 주므로 `ItunesMusicSearchService`
가 문자열로 받아 `ObjectMapper` 로 파싱한다. 다른 무인증 후보로 Deezer(`api.deezer.com/search`) 도 있음.
(Spotify 로 가려면 앱 소유 계정을 Premium 으로 만들고 `MusicSearchService` 구현체만 되돌리면 됨.)

### 영상 카드/일정 리스트에 "제목"이 안 뜬다

**원인.** 백엔드 `Media` · `Schedule` 엔티티에 title 필드가 없다 (Media = URL·종류·공개범위,
Schedule = 종류·일시·장소). 목업엔 있던 제목 입력칸을 프론트에서 뺐다.

**동작.** 영상은 URL 을 짧게 줄인 문자열(`mappers.toMedia` 의 `prettyUrl`)을 라벨로 쓰고 클릭 시 원본으로 연다.
일정은 `장소`(없으면 "연습"/"공연" 라벨)를 헤딩으로 쓴다.

### 일정 캘린더가 이번 달이 아니라 다른 달로 열린다

**원인.** 버그 아님. `SchedulePage` 는 데이터가 들어오면 "지금 이후 가장 가까운 일정"이 있는 달로
`view` 를 한 번 맞춘다 (`lib/schedule.nextSchedule`). 다가오는 일정이 없으면 마지막 일정의 달.
등록된 일정이 하나도 없으면 실제 이번 달로 연다.

### 파트 배정 select 를 바꿔도 반영이 안 되고 콘솔에 `SONG_NOT_CONFIRMED`

**원인.** `PUT /api/songs/{id}/parts/{partId}/assign` 은 곡이 `CONFIRMED` 여야 한다(그 외 409).
UI 는 `CONFIRMED` 곡에서만 select 를 보여주므로 보통은 안 나지만, 승격 직후 응답 반영 전 클릭하면 날 수 있다.

**참고.** 프론트는 슬롯키(`"기타#2"`)를 `Song.parts` 에서 `partId` 로, 멤버 이름을 `members` 에서
`userId` 로 되짚어 호출한다. 동명이인이 있으면 첫 번째 멤버로 배정된다(엣지 케이스).

### 곡/일정/영상이 로그인 전에도 보이는데 액션만 막힌다

정상. GET 은 전부 공개라 게스트도 목록을 받는다. 투표·등록·출결·배정은 `useGuard` 가 로그인 모달을 띄우고,
서버도 비회원 액션은 401/403 으로 막는다. `media` 는 비회원에게 `LINK_PUBLIC` 만 내려온다(공개범위 필터).

### 영상을 일정에 연결해 등록했는데 그 일정 상세 "이 일정의 영상" 에 안 뜬다

**증상.** 영상 첨부 모달에서 "연결할 일정" 을 고르고 등록 → 영상 카드엔 "일정 · …" 표시가 되는데,
일정 탭의 그 일정 상세에는 "아직 연결된 영상이 없습니다" 로 남는다. 새로고침하면 정상.

**원인.** `AppContext.addMedia` 가 `media` state 만 갱신하고, 연결된 일정의 `mediaIds`(= `schedules` state)는
그대로 뒀다. 일정 상세는 `schedules` 에서 `mediaIds` 를 읽으므로 반영이 안 됨. `removeMedia` 도 같은 문제.

**해결 (2026-09-03, feature/8).** `addMedia`(`scheduleId` 있을 때) / `removeMedia`(지우는 영상이 일정에 연결돼
있었을 때) 후 `scheduleApi.listSchedules` 로 `schedules` 를 다시 받는다 (`refreshSchedules` 헬퍼).
백엔드는 처음부터 `ScheduleResponse.media` 를 정상으로 내려주고 있었음 — 순수 클라 상태 동기화 버그였다.
