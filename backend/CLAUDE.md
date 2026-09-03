# 밴디브 (Bandive) — 백엔드

밴드 전용 아카이브형 커뮤니티 서비스. 노션 대체용으로, 밴드를 만들고 멤버를 초대해
합주곡 리스트 / 일정 / 구성원 / 영상을 한곳에서 관리한다.

- 대상: 실제 지인 밴드 소규모 배포 (10팀 이하)
- 핵심 차별점: 영상 아카이브를 밴드/곡/일정과 연결된 메타데이터로 정리·검색
  (영상 자체는 자체 호스팅 X, 외부 URL 첨부 방식)
- 저장소 구조: `Bandive/backend` (이 디렉토리) + `Bandive/frontend` (React, 목업 이식 완료)

---

## 기술 스택 / 확정 결정사항

| 항목 | 값 |
|---|---|
| Spring Boot | **4.1.1** (Spring Framework 7, Jakarta EE 11). 그대로 4.x 진행 |
| Java | 21 / Gradle 9.7.1 (Groovy DSL) |
| base package | `com.bandive.bandive` |
| 설정 파일 | `application.yaml` (+ `-local` / `-test` / `-prod`). 기본 프로파일 `local` |
| DB | PostgreSQL — 로컬 docker-compose 는 **호스트 5433**, 앱은 **8081** (기본 포트가 타 프로젝트와 충돌) |
| 캐시 | Redis (초대 코드 캐싱, refresh 토큰 저장) — 로컬 docker-compose **호스트 6380** |
| **인증** | **JWT (access 15~30분 + refresh)**. refresh 는 Redis 저장 + httpOnly 쿠키. 카카오 소셜 로그인만 지원 |
| **패키지 구조** | **도메인형** — `com.bandive.bandive.<domain>` 안에 `controller / service / repository / dto / entity`. 공통은 `common/{config,exception,response}` |
| **스키마 관리** | **Flyway** 마이그레이션 (`V1__init.sql` 부터 손으로 — Phase 1). `spring.jpa.hibernate.ddl-auto=validate` |
| 빌드 의존성 | `spring-boot-flyway`(⚠️ `flyway-core` 아님 — 아래), `flyway-database-postgresql`, `jjwt-api/impl/jackson`, `io.spring.javaformat` 플러그인 |
| 테스트 | Testcontainers **2.x** (아티팩트명 `testcontainers-*` 접두, `PostgreSQLContainer` 는 `org.testcontainers.postgresql` 의 비제네릭 클래스). 통합 테스트 베이스 = `support/IntegrationTest` |
| 스타터 주의 | Boot 4 → `starter-web` = `starter-webmvc`, 테스트 스타터 모듈별 분리, **auto-config 도 모듈 분리** (Flyway 는 `spring-boot-flyway` 를 직접 넣어야 동작) |

---

## 핵심 도메인 규칙

- 권한은 **밴드장(OWNER) / 사용자(MEMBER) 2단계만**. 관리자 역할 없음
- 로그인: **카카오 소셜 로그인만**
- **비회원도 전체 콘텐츠 열람(GET) 가능.** 투표/등록 등 액션(POST/PATCH/DELETE)만 로그인 필요
  → Spring Security 에서 공개 GET / 인증 액션을 명확히 분리. 밴드장 전용은 메서드 단위 권한 어노테이션
- 한 사용자가 여러 밴드 소속 가능 (BandMember 다대다)
- 밴드 가입은 **초대 코드/링크로만** (공개 검색/가입 없음)
- 곡(Song) 등록 2가지: 외부 음원 API 검색(`source_type=SEARCH`) / 직접 입력(`source_type=MANUAL`)
  - 부가 필드: `memo`, `reference_video_url`, 세션 구성(SongPart)
- 곡 상태: `WISHLIST` → `CONFIRMED`. 승격은 자동 아님, **밴드장이 득표수 참고해 수동 승인**
- 투표(Vote): 곡당 사용자 1인 1표, 유니크 제약
- SongPart: 곡 등록 시점에 세션 슬롯 생성. `assigned_member_id` 는 `CONFIRMED` 상태에서만 값 배정 가능 (그 외 null)
  - ⚠️ 프론트 목업은 "로그인한 멤버 누구나 배정" 으로 동작함. 기획서 API 표는 "밴드장". **백엔드 정책은 구현 시 확정** (일단 밴드장 기준으로 가되 논의)
- 영상(Media): 자체 파일 X, `external_url` (유튜브/구글드라이브 등)만 저장
  - `visibility` 밴드별 설정 (밴드 멤버만 vs 링크 소지자). 비회원 GET 시 `멤버만` 영상 제외
  - `schedule_id` 로 특정 일정과 선택적 연결 (nullable)
- 로고/배너: 이미지 파일 직접 업로드 (자체 스토리지). `StorageService` 추상화 — 로컬 디스크(dev) / S3(prod)

---

## 데이터 스키마 (ERD)

### USERS
- id (PK), nickname, email, created_at
- (+ 카카오 식별용) kakao_id (unique)

### BANDS
- id (PK), name, logo_url, banner_url, created_at

### BAND_MEMBERS  (User ↔ Band 다대다)
- id (PK), band_id (FK), user_id (FK), role (OWNER/MEMBER), joined_at
- unique (band_id, user_id)

### INVITE_CODES
- id (PK), band_id (FK), code, expires_at, max_uses, used_count

### SONGS
- id (PK), band_id (FK), title, artist, status (WISHLIST/CONFIRMED),
  source_type (SEARCH/MANUAL), external_track_id (nullable),
  memo, reference_video_url, added_by (FK User)

### SONG_PARTS
- id (PK), song_id (FK), instrument (DRUM/GUITAR/KEYBOARD/VOCAL/BASS 등),
  part_index (동일 악기 내 순번), assigned_member_id (FK BandMember, nullable)

### VOTES  (Song ↔ User 다대다, 좋아요 방식)
- id (PK), song_id (FK), user_id (FK)
- unique (song_id, user_id)

### SCHEDULES
- id (PK), band_id (FK), type (REHEARSAL/PERFORMANCE), date_time, location, created_by (FK User)

### ATTENDANCES  (Schedule ↔ User 다대다)
- id (PK), schedule_id (FK), user_id (FK), status (참석/불참/미정)
- unique (schedule_id, user_id)

### MEDIA
- id (PK), band_id (FK), schedule_id (FK, nullable), type (REHEARSAL/PERFORMANCE),
  external_url, platform, visibility, uploaded_by (FK User)

---

## API 엔드포인트

### 인증
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | /api/auth/kakao/callback | 카카오 로그인 콜백, 토큰 발급 | X |
| POST | /api/auth/logout | 로그아웃 (refresh 무효화) | O |
| POST | /api/auth/refresh | access 토큰 재발급 | O (refresh 쿠키) |

### 밴드
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | /api/bands | 밴드 생성 | O |
| GET | /api/bands/{bandId} | 밴드 상세 | X |
| GET | /api/bands/my | 내가 속한 밴드 목록 | O |
| PATCH | /api/bands/{bandId} | 정보 수정 (이름/설명) | O (밴드장) |
| POST | /api/bands/{bandId}/logo | 로고 업로드 | O (밴드장) |
| POST | /api/bands/{bandId}/banner | 배너 업로드 | O (밴드장) |

### 초대
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | /api/bands/{bandId}/invite-codes | 초대 코드 발급/재발급 | O (밴드장) |
| POST | /api/invite-codes/{code}/join | 초대 코드로 가입 | O |

### 밴드 멤버
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | /api/bands/{bandId}/members | 멤버 목록 | X |
| DELETE | /api/bands/{bandId}/members/{userId} | 멤버 추방 | O (밴드장) |
| DELETE | /api/bands/{bandId}/members/me | 밴드 탈퇴 | O |

### 곡
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | /api/songs/search?q= | 외부 음원 API 곡 검색 | X |
| GET | /api/bands/{bandId}/songs?status= | 곡 목록 (위시/확정 필터) | X |
| POST | /api/bands/{bandId}/songs | 곡 추가 (메모/세션구성/참고영상 포함) | O |
| POST | /api/songs/{songId}/vote | 투표 (1인 1표) | O |
| DELETE | /api/songs/{songId}/vote | 투표 취소 | O |
| PATCH | /api/songs/{songId}/confirm | 합주곡으로 승격 | O (밴드장) |
| PUT | /api/songs/{songId}/parts/{partId}/assign | 파트 배정/해제 | O (정책 확정 필요) |
| DELETE | /api/songs/{songId} | 곡 삭제 | O (밴드장) |

### 일정
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | /api/bands/{bandId}/schedules | 일정 목록 (연결 영상 포함) | X |
| POST | /api/bands/{bandId}/schedules | 일정 등록 | O |
| PATCH | /api/schedules/{scheduleId} | 일정 수정 | O |
| DELETE | /api/schedules/{scheduleId} | 일정 삭제 | O (밴드장) |
| POST | /api/schedules/{scheduleId}/attendance | 참석 여부 등록/변경 | O |

### 미디어
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | /api/bands/{bandId}/media?scheduleId= | 영상 목록 (일정 필터, 공개범위 적용) | X (공개 설정에 따라 제한) |
| POST | /api/bands/{bandId}/media | 영상 URL 등록 (scheduleId 선택) | O |
| PATCH | /api/media/{mediaId}/visibility | 공개 범위 변경 | O (밴드장) |
| DELETE | /api/media/{mediaId} | 영상 삭제 | O |

---

## 작업 로드맵

각 Phase(Phase 4는 도메인별)마다 **착수 전에 구체적으로 뭘 할지 설명하고 사용자 승인받은 뒤 진행**한다.

- **Phase 0 — 부트스트랩 세팅 ✅ 완료 (2026-09-01)**: 도메인형 패키지 골격(`.gitkeep`),
  `application.yaml` 프로파일 분리(local/test/prod), `docker-compose.yml`(Postgres 5433 / Redis 6380),
  `.gitignore`/`.env.example`, Flyway 도입(`spring-boot-flyway`), Testcontainers 통합 테스트 베이스,
  `io.spring.javaformat`, `/actuator/health` 부팅 확인 완료. `db/migration/` 은 비어있음(V1 은 Phase 1).
  git 협업 세팅(pre-commit 훅 `hooks/`, 이슈/PR 템플릿, GitHub Actions CI)은 루트 [`../README.md`](../README.md) 참고.
  겪은 이슈는 [`../docs/TROUBLESHOOTING.md`](../docs/TROUBLESHOOTING.md).
- **Phase 1 — 도메인 모델 ✅ 완료 (2026-09-01)**: 엔티티 10개 + Enum 9개 + `BaseTimeEntity`(Auditing, `Instant` created/updated) +
  유니크 제약 + `V1__init.sql`(FK 전부 CASCADE, 선택 연결만 SET NULL) + Repository 10개.
  검증: `@DataJpaTest`(`support/RepositoryTest`, `@Import(JpaConfig)` 필수) + `support/AbstractContainerTest` 싱글턴 컨테이너.
  결정: `updated_at` 전 테이블, `Song↔SongPart` 만 양방향+cascade/orphanRemoval, `InviteCode.code` 전역 unique,
  `SongPart` unique(song,instrument,part_index). 파트 배정 권한(밴드장 vs 누구나)은 Phase 4 에서 확정.
- **Phase 2 — 인증(카카오 OAuth2 + JWT) ✅ 완료 (2026-09-01)**: `SecurityConfig`(STATELESS, `GET /api/**`+`/error` 공개, 그 외 인증),
  카카오 `oauth2Login`(authorization request 는 쿠키 = `HttpCookieOAuth2AuthorizationRequestRepository`),
  `CustomOAuth2UserService`(kakao_id upsert, 이메일 미수집), `OAuth2LoginSuccessHandler`(refresh → Redis `refresh:{userId}` + httpOnly `SameSite=Strict` 쿠키 `refresh_token`, path `/api/auth` → 프론트 `?oauth/success` 리다이렉트).
  `AuthController`: `POST /api/auth/refresh`(회전), `POST /api/auth/logout`, `GET /api/auth/me`.
  `JwtProvider`(HS256, access 30m / refresh 14d), `JwtAuthenticationFilter`(Bearer), `@CurrentUser Long`, `BandGuard`(`@PreAuthorize("@bandGuard.isOwner(#bandId)")` + `@EnableMethodSecurity`), 401/403 JSON 핸들러.
  설정: `spring.config.import: optional:file:.env[.properties]` 로 `.env` 로드. `app.auth.*` = `AuthProperties`.
  검증: 41 테스트 (JwtProvider / SecurityRules / AuthController / CustomOAuth2UserService / OAuth2LoginSuccessHandler / BandGuard). 실제 카카오 없이 더미값 + `MockWebServer` 미사용(핸들러 직접 호출).
- **Phase 3 — 공통 인프라 ✅ 완료 (2026-09-02)**:
  - `common/exception/`: `BandiveException`(status+code, 직접 throw 가능) + `NotFoundException`(404)/`ForbiddenException`(403)/`ConflictException`(409)/`ValidationException`(400).
  - `GlobalExceptionHandler`(`@RestControllerAdvice extends ResponseEntityExceptionHandler`): BandiveException / `@Valid` 실패(첫 필드 메시지 한 줄) / `AuthorizationDeniedException` / 프레임워크 예외(405·깨진 JSON·404) / 그 외 → 500 (내부 메시지 은닉).
  - `common/response/ErrorResponse`(status, code, message, path, timestamp). **성공 응답은 래핑 안 함**.
  - 필터단(401/403)은 `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` 가 `ObjectMapper` 로 같은 `ErrorResponse` 를 씀. `AuthController` 는 `ResponseStatusException` → `BandiveException` 으로 교체.
  - CORS: `common/config/{CorsConfig,CorsProperties}` 로 분리. `app.cors.allowed-origins`(리스트, `${APP_CORS_ALLOWED_ORIGINS}`), `allowCredentials(true)`. `SecurityConfig` 는 `.cors(withDefaults())` 만.
  - DTO 컨벤션: `common/dto/package-info.java` (Request=record+Bean Validation, Response=record+`from(Entity)`, 엔티티 직접 노출 금지).
  - ⚠️ Boot 4 = **Jackson 3** → `tools.jackson.*` 패키지. 검증: 49 테스트 (GlobalExceptionHandler 6 / CorsConfig 2 신규).
- **Phase 4 — 도메인 API** (각 도메인이 승인 단위). 각 도메인: DTO → Service → Controller → `@WebMvcTest`
  - **4-1 밴드 ✅ 완료 (2026-09-02)**: `band/{dto,service,controller}`. `POST /api/bands`(생성자 자동 OWNER 등록, 201) /
    `GET /api/bands/{id}`(공개) / `GET /api/bands/my`(인증) / `PATCH /api/bands/{id}`(`@PreAuthorize("@bandGuard.isOwner(#bandId)")`) /
    `POST .../logo` · `POST .../banner`(밴드장, multipart). `BandResponse` 에 `memberCount` 포함.
    **V2**: `bands.description varchar(500)` nullable. `BandMemberRepository.countByBandId` 추가.
    파일: `common/storage/{StorageService,LocalStorageService,StorageProperties}` (인터페이스 뒤 로컬디스크 구현, Phase 5 에서 S3 교체).
    `WebMvcConfig` 가 `/files/**` → 로컬 업로드 디렉토리 서빙 (SecurityConfig permitAll). `spring.servlet.multipart` 5MB.
    `SecurityConfig`: `GET /api/bands/my` 인증, `/files/**` 공개 추가. 테스트: `BandControllerTest`(@WebMvcTest) 7 + `BandServiceTest`(RepositoryTest) 7. 총 64 그린.
  - **4-2 초대 ✅ 완료 (2026-09-02)**: `invite/{dto,service,controller}`. `POST /api/bands/{bandId}/invite-codes`(밴드장, 201, `InviteCodeResponse{code,inviteUrl,...}`) / `POST /api/invite-codes/{code}/join`(로그인 → `BandResponse`).
    밴드당 코드 1개(재발급 시 이전 행 삭제+캐시 evict). 코드 8자리(`0O1I` 제외), 만료·횟수 제한 없음(컬럼 유지, `used_count`만 +1).
    join: 없는 코드 `404 INVITE_CODE_NOT_FOUND` / 이미 멤버 `409 ALREADY_MEMBER`. `InviteCodeCache`(Redis `invite:{code}→bandId`, TTL 없음, 미스 시 DB 복구).
    신규 `app.frontend.base-url`(`common/config/{FrontendProperties,AppConfig}`), `InviteCodeRepository` 에 `existsByCode/findByBandId/deleteByBandId`. SecurityConfig 수정 없음. 74 테스트 그린.
  - **4-3 밴드 멤버 ✅ 완료 (2026-09-02)**: `member/{dto,service,controller}`.
    `GET /api/bands/{bandId}/members`(공개, `MemberResponse{userId,nickname,role,parts,joinedAt}`, OWNER 먼저→가입순, `findAllByBandIdWithUser` fetch join) /
    `PATCH .../members/me`(내 파트) / `PATCH .../members/{userId}`(밴드장이 남의 파트) /
    `DELETE .../members/{userId}`(추방, 밴드장 204) / `DELETE .../members/me`(탈퇴 204) /
    `PUT /api/bands/{bandId}/owner`(밴드장 위임, `{userId}`, 이전 밴드장→MEMBER, 204) / `DELETE /api/bands/{bandId}`(밴드 삭제, 204).
    추방: 없는 멤버 `404 MEMBER_NOT_FOUND`, 밴드장 대상 `409 CANNOT_KICK_OWNER`. 탈퇴: 비멤버 `404 NOT_A_MEMBER`, 밴드장 `409 OWNER_CANNOT_LEAVE`.
    **V3**: `band_member_parts` 테이블(멤버 1:N 파트, `@ElementCollection Set<String>`). Instrument enum 이름 기본이나 자유 문자열 허용(공백/중복 제거).
    밴드 삭제: `bands` FK 전부 `ON DELETE CASCADE` → `bandRepository.delete()` 로 하위 일괄. 로고/배너 파일·Redis 초대키는 `BandService.delete` 가 직접 정리(→ `InviteCodeRepository`+`InviteCodeCache` 주입).
    `/me` literal 우선 매칭. SecurityConfig 수정 없음(전부 non-GET). 99 테스트 그린.
  - **4-4 곡 ✅ 완료 (2026-09-02)**: `song/{dto,service,controller}`.
    `GET /api/songs/search?q=`(공개, **스텁** `StubMusicSearchService` — 쿼리 echo 3건. Phase 5 실 API) /
    `GET /api/bands/{bandId}/songs?status=`(공개, `SongResponse` 에 `voteCount`·`votedByMe`·`parts` 배치조회 fetch-join) /
    `POST .../songs`(밴드 멤버, 세션 구성 `[{instrument,count}]` → SongPart 슬롯 생성, 항상 WISHLIST) /
    `POST|DELETE /api/songs/{id}/vote`(**멱등**, `{voteCount,votedByMe}` 반환) /
    `PATCH /api/songs/{id}/confirm`(밴드장, WISHLIST→CONFIRMED) /
    `PUT /api/songs/{id}/parts/{partId}/assign`(**밴드 멤버 누구나**, `{userId}` 또는 null, **곡이 CONFIRMED 여야** — 아니면 `409 SONG_NOT_CONFIRMED`) /
    `DELETE /api/songs/{id}`(밴드장, parts·votes cascade).
    **`SongPart.instrument` 는 `String` 자유 문자열** (Instrument enum 삭제, member.part 와 동일 정책. V1 컬럼이 이미 varchar(20) → 마이그레이션 없음).
    권한은 서비스 레벨(songId → song.band 로 멤버/밴드장 검사). `NOT_A_MEMBER` 403 / `NOT_BAND_OWNER` 403.
    ⚠️ 공개 GET + 현재유저 → `@AuthenticationPrincipal UserPrincipal`(익명이면 null), `@CurrentUser` 는 익명에서 500.
    124 테스트 그린.
  - **4-5 일정 + 출결 ✅ 완료 (2026-09-02)**: `schedule/{dto,service,controller}`.
    `GET /api/bands/{bandId}/schedules`(공개, dateTime 오름차순, `ScheduleResponse` 에 `attendees`(응답한 멤버만)·`counts{attending,absent,undecided}`·`myStatus`(비회원 null) — `@AuthenticationPrincipal UserPrincipal`) /
    `POST .../schedules`(밴드 멤버, `{type,dateTime,location?}`) /
    `PATCH /api/schedules/{id}`(**밴드 멤버 누구나**, 부분 수정 — null 필드 무시) /
    `DELETE /api/schedules/{id}`(밴드장, attendances cascade) /
    `POST /api/schedules/{id}/attendance`(밴드 멤버, `{status}` → 내 출결 **upsert**).
    권한 서비스 레벨(scheduleId → schedule.band). 없는 일정 `404 SCHEDULE_NOT_FOUND`.
    **"연결 영상 포함"은 4-6 에서** (Media 도메인 후). 마이그레이션 없음. 142 테스트 그린.
  - **4-6 미디어 ✅ 완료 (2026-09-02) → Phase 4 전체 완료**: `media/{dto,service,controller}`.
    `GET /api/bands/{bandId}/media?scheduleId=`(공개, **공개범위 필터**: 밴드 멤버는 전부 / 비회원·비멤버는 `LINK_PUBLIC` 만) /
    `POST .../media`(밴드 멤버, `{externalUrl, type, visibility?, scheduleId?}` — `platform` 은 URL 로 자동 판별 `MediaPlatform.detect`, 기본 visibility `MEMBERS_ONLY`, scheduleId 는 같은 밴드여야 — 아니면 `400 SCHEDULE_BAND_MISMATCH`) /
    `PATCH /api/media/{id}/visibility`(밴드장) / `DELETE /api/media/{id}`(**등록자 본인 또는 밴드장**).
    **4-5 에서 미룬 것 완성**: `ScheduleResponse.media` 추가 (같은 공개범위 필터). `ScheduleService` 에 `MediaRepository` 주입.
    마이그레이션 없음. **159 테스트 그린.**
- **Phase 5 — 외부 음원 검색 ✅ 완료 (2026-09-03)**: `song/service/ItunesMusicSearchService`
  (Apple iTunes Search API — 인증·API 키 불필요. 응답이 `Content-Type: text/javascript` 라 문자열로 받아 `ObjectMapper` 로 파싱.
  외부 장애·깨진 본문은 빈 목록으로 삼킴). `app.music.provider=itunes` 면 활성, 그 외 모든 값은 `StubMusicSearchService`
  (`MusicSearchConfig` 의 두 `@Bean` + `@ConditionalOnMissingBean` fallback). `song/config/{MusicProperties(provider,limit,country),MusicSearchConfig}`.
  `.env` 는 `MUSIC_PROVIDER`(+옵션 `MUSIC_COUNTRY`) 한 줄. `ItunesMusicSearchServiceTest`(MockRestServiceServer) 4개. 총 163 그린.
  실검색 스모크 통과: `GET /api/songs/search?q=yesterday` → 실 트랙, SEARCH 타입 곡 추가 OK.
  ⚠️ Spotify 는 `/v1/search` 가 앱 소유 계정 Premium 필요(무료계정 403, 2026-09-03 확인)라 배제. 앨범아트는 추후 (iTunes `artworkUrl100` 로 가능).
  → **S3 `StorageService` 는 Phase 7(배포)로 이동** (로컬은 `LocalStorageService` 로 충분, 버킷·IAM 은 배포 인프라와 함께).
- **Phase 6 — 프론트 연동 ✅ 완료 (2026-09-03)**: `frontend/src/api/*` 레이어, `AppContext` 액션 전부 실제 호출, 목 제거.
  곡/일정/미디어까지 연동 + 브라우저 E2E 확인. (곡 검색 UI 는 stub 이든 spotify 든 그대로 동작 — 응답 형태 동일)
- **Phase 7 — 배포**: Dockerfile, prod compose, CI/CD (push → AWS 자동 배포).
  **+ S3 `StorageService` 구현체** (Phase 5 에서 이동 — `app.storage.type=s3`, AWS SDK v2, 버킷·IAM).
  ⚠️ 로컬에서 비켜쓴 포트를 **기본값으로 복구**: Postgres 5432, Redis 6379, 앱 8080
  (`application-prod.yaml` / prod compose / `SERVER_PORT`). 프로덕션 호스트엔 충돌 없음.

---

## 프론트엔드 참고 (`../frontend`)

- Vite + React 19 + TS + react-router-dom 7, 플레인 CSS. 목업 이식 **완료** (홈/곡/일정/영상/멤버 5개 화면).
- 현재 밴드 스코프 상태를 `src/store/AppContext.tsx` 가 목 데이터로 들고 있음.
  액션(`voteSong`, `promoteSong`, `assignPart`, `addSong`, `addMedia`, `kickMember`, `regenerateInviteCode`)을
  백엔드 API 호출로 교체하는 게 Phase 6.
- 인증은 `AppContext.login/logout` + `src/hooks/useGuard.ts`(비회원 액션 → 로그인 모달).
