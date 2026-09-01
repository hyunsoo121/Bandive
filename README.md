# 밴디브 (Bandive)

밴드 전용 아카이브형 커뮤니티 서비스. 밴드를 만들고 멤버를 초대해 **합주곡 리스트 / 일정 / 구성원 / 영상**을
한곳에서 관리한다. 노션 대체가 목표이고, 영상은 자체 호스팅하지 않고 외부 URL 을 메타데이터로 정리·검색한다.

- 대상: 실제 지인 밴드 소규모 배포 (10팀 이하)
- 권한 2단계: **밴드장(OWNER) / 멤버(MEMBER)**. 로그인은 카카오만. 비회원도 열람(GET)은 가능

## 저장소 구조 (모노레포)

| 경로 | 내용 |
|---|---|
| `frontend/` | Vite + React 19 + TS + react-router-dom 7, 플레인 CSS. 목업 이식 완료 (홈/곡/일정/영상/멤버) |
| `backend/` | Spring Boot 4.1.1 (Java 21) · PostgreSQL · Redis · JWT(카카오 OAuth2) · Flyway. 상세: [`backend/CLAUDE.md`](backend/CLAUDE.md) |
| `밴드 아카이브 서비스 UIUX/` | 원본 목업 (Claude Design 캔버스) |
| `hooks/` | git pre-commit 훅 (lint/format) |
| `docs/` | [트러블슈팅](docs/TROUBLESHOOTING.md) 등 |

## 처음 클론했다면

```sh
sh hooks/install.sh        # git pre-commit 훅 활성화 (core.hooksPath=hooks)
```

### 프론트엔드

```sh
cd frontend
npm ci
npm run dev                # http://localhost:5173
```

| 스크립트 | 설명 |
|---|---|
| `npm run dev` / `build` / `preview` | Vite |
| `npm run lint` / `lint:fix` | oxlint |
| `npm run format` / `format:check` | Prettier |

### 백엔드

로컬 인프라(Postgres 5433, Redis 6380)를 먼저 띄운다. **포트가 기본값(5432/6379/8080)이 아닌 이유는
다른 프로젝트와 충돌을 피하기 위함** — [트러블슈팅](docs/TROUBLESHOOTING.md#포트-충돌) 참고.

```sh
cd backend
docker compose up -d       # postgres:5433, redis:6380
./gradlew bootRun          # http://localhost:8081  (local 프로파일이 기본)
curl localhost:8081/actuator/health
```

| 명령 | 설명 |
|---|---|
| `./gradlew bootRun` | 앱 실행 (local 프로파일) |
| `./gradlew check` | 포맷 검사 + 통합 테스트 (Docker 필요 — Testcontainers) |
| `./gradlew format` / `checkFormat` | spring-javaformat |
| `docker compose down` (`-v` 볼륨까지) | 인프라 정리 |

프로파일: `local`(기본, docker-compose 대상) / `test`(Testcontainers) / `prod`(전 항목 환경변수 필수).
환경변수는 [`backend/.env.example`](backend/.env.example) 참고.

## 개발 워크플로

1. 이슈 생성 → 브랜치 분기 (`feat/…`, `fix/…`, `chore/…`)
2. 커밋 시 pre-commit 훅이 변경된 영역(frontend/backend)만 lint·format 검사
3. PR 올리면 GitHub Actions CI 가 프론트(lint/format/build)·백엔드(checkFormat/build) 실행
4. `main` 병합

## 로드맵

`backend/CLAUDE.md` 의 Phase 0~7. 현재 **Phase 1(도메인 모델) 완료** — 엔티티 10개 + Enum + `V1__init.sql` + Repository + `@DataJpaTest`. 다음은 Phase 2(카카오 OAuth2 + JWT).

> 개발 중 `V1__init.sql` 을 수정했다면 Flyway checksum 때문에 로컬 DB 를 초기화해야 한다:
> `cd backend && docker compose down -v && docker compose up -d` ([트러블슈팅](docs/TROUBLESHOOTING.md) 참고).
