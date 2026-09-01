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
| 설정 파일 | `src/main/resources/application.yaml` |
| DB | PostgreSQL |
| 캐시 | Redis (초대 코드 캐싱, refresh 토큰 저장) |
| **인증** | **JWT (access 15~30분 + refresh)**. refresh 는 Redis 저장 + httpOnly 쿠키. 카카오 소셜 로그인만 지원 |
| **패키지 구조** | **도메인형** — `com.bandive.bandive.<domain>` 안에 `controller / service / repository / dto / entity` |
| **스키마 관리** | **Flyway** 마이그레이션 (`V1__init.sql` 부터 손으로). `spring.jpa.hibernate.ddl-auto=validate` |
| 빌드에 추가 필요 | `flyway-core`, `flyway-database-postgresql`, `jjwt-api/impl/jackson` |
| 스타터 주의 | Boot 4 → `spring-boot-starter-web` 이 `spring-boot-starter-webmvc` 로, 테스트 스타터가 모듈별 분리됨 |

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

- **Phase 0 — 부트스트랩 세팅**: 도메인형 패키지 골격, `application.yaml` 프로파일 분리(local/prod),
  `docker-compose.yml`(Postgres+Redis), `.gitignore`/`.env.example`, Flyway 도입, `/actuator/health` 부팅 확인,
  build.gradle 에 Flyway·JJWT 추가
- **Phase 1 — 도메인 모델**: 엔티티 10개 + Enum + `BaseTimeEntity`(JPA Auditing) + 유니크 제약 +
  `V1__init.sql` + Repository. 검증: `@DataJpaTest`
- **Phase 2 — 인증(카카오 OAuth2 + JWT)**: `SecurityFilterChain`(공개 GET / 인증 액션 분리),
  카카오 provider, `CustomOAuth2UserService`(kakao_id 로 User upsert), 로그인 성공 핸들러 → JWT 발급,
  `/api/auth/logout`·`/refresh`, `@PreAuthorize("@bandGuard.isOwner(#bandId)")` 커스텀 빈. 검증: 통합 테스트(카카오 목)
- **Phase 3 — 공통 인프라**: CORS(프론트 오리진), `@RestControllerAdvice` 전역 예외 → 일관 에러 JSON,
  DTO 컨벤션, `BandGuard`(소속/밴드장 검사)
- **Phase 4 — 도메인 API** (CLAUDE.md 순서, 각 도메인이 승인 단위):
  1. 밴드  2. 초대(Redis 캐시)  3. 밴드 멤버  4. 곡(+SongPart 슬롯·투표·승격·배정)
  5. 일정(+출결)  6. 미디어(visibility 필터)
  각 도메인: DTO → Service → Controller → `@WebMvcTest`
- **Phase 5 — 파일 업로드 / 외부 음원 검색**: `StorageService`(로컬 dev / S3 prod),
  곡 검색은 MANUAL 우선 완성 · SEARCH 는 스텁 후 실 API 연동
- **Phase 6 — 프론트 연동**: `frontend/src/api/*` 레이어, `AppContext` 액션을 실제 호출로 교체, 목 제거
- **Phase 7 — 배포**: Dockerfile, prod compose, CI/CD (push → AWS 자동 배포)

---

## 프론트엔드 참고 (`../frontend`)

- Vite + React 19 + TS + react-router-dom 7, 플레인 CSS. 목업 이식 **완료** (홈/곡/일정/영상/멤버 5개 화면).
- 현재 밴드 스코프 상태를 `src/store/AppContext.tsx` 가 목 데이터로 들고 있음.
  액션(`voteSong`, `promoteSong`, `assignPart`, `addSong`, `addMedia`, `kickMember`, `regenerateInviteCode`)을
  백엔드 API 호출로 교체하는 게 Phase 6.
- 인증은 `AppContext.login/logout` + `src/hooks/useGuard.ts`(비회원 액션 → 로그인 모달).
