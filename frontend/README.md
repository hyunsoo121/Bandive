# 밴디브 프론트엔드

목업(`../밴드 아카이브 서비스 UIUX/밴드 아카이브.dc.html`)을 리액트로 옮긴 것.

## 스택

- Vite + React 19 + TypeScript
- react-router-dom 7
- 플레인 CSS (토큰 `src/styles/tokens.css` + 공용 `src/styles/shared.css` + 컴포넌트별 `.css`)
- 상태: React Context (`src/store/AppContext.tsx`)

## 실행

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # tsc -b && vite build
```

백엔드는 `../backend` 를 **8081 포트**로 띄워둔다 (`backend/.env` 에 `KAKAO_*`, `JWT_SECRET` 필요).
프론트는 `VITE_API_ORIGIN`(기본 `http://localhost:8081`)으로 백엔드를 직접 호출한다 —
백엔드 CORS 가 `http://localhost:5173` + credentials 를 허용하므로 dev 프록시는 쓰지 않는다.
배포 시엔 `.env.local` 에 `VITE_API_ORIGIN` 을 실제 백엔드 주소로 지정한다 (`.env.example` 참고).

## 구조

```
src/
  styles/       tokens.css (모더니스트 디자인 시스템), shared.css (버튼/세그/모달 등 프리미티브)
  types.ts      도메인 타입 (ERD 대응)
  api/          client.ts (fetch 래퍼 + 토큰/401 재시도), auth/bands/invites/members.ts,
                types.ts (백엔드 DTO), mappers.ts (DTO → 도메인 타입)
  mock/         data.ts (곡/영상/일정 시드 — 아직 미연동), selectors.ts (일정 조회 헬퍼)
  store/        AppContext.tsx (세션·현재밴드·멤버·초대 = API 연동 / 곡·영상·일정 = mock)
  hooks/        useGuard.ts (비회원 액션 → 로그인 모달)
  lib/          nav.ts (탭/사이드바 정의), songs.ts (세션 슬롯 계산)
  components/    AppLayout(셸), BandSwitcher, LoginModal, CreateBandModal, Modal, Fab,
                AddSongModal, AddMediaModal, DevRoleBar, ...
  pages/        HomePage, SongsPage, SchedulePage, MediaPage, MembersPage,
                SystemPages.tsx (HomeRedirect / OAuthSuccess / OAuthFailure / InviteJoin)
```

## 라우팅

- `/` → `HomeRedirect`: 세션 복구 후 내 첫 밴드로. 밴드 없으면 로그인/생성 안내
- `/oauth/success` · `/oauth/failure` — 카카오 로그인 복귀 지점
- `/invite/:code` — 초대 링크. 로그인돼 있으면 바로 가입 후 해당 밴드로
- `/bands/:bandId` (AppLayout) 하위: index=홈, `songs` `schedule` `media` `members`

## API 연동 현황

| 도메인                                            | 상태                                     |
| ------------------------------------------------- | ---------------------------------------- |
| 인증 (카카오 OAuth2 → refresh 쿠키 → access 토큰) | ✅ 연동                                  |
| 밴드 (내 밴드 목록 / 상세 / 생성)                 | ✅ 연동                                  |
| 멤버 (목록 / 추방)                                | ✅ 연동                                  |
| 초대 (코드 발급·재발급 / 코드로 가입)             | ✅ 연동                                  |
| 곡 · 일정 · 영상                                  | ⏳ mock 유지 (백엔드 Phase 4-4~4-6 대기) |

- **인증 플로우**: `login()` → `{API}/oauth2/authorization/kakao` 로 페이지 이동 → 카카오 →
  백엔드가 refresh 를 httpOnly 쿠키로 심고 `/oauth/success` 로 리다이렉트 →
  앱 부팅 시 `POST /api/auth/refresh` 로 access 토큰 획득(메모리 보관) → `GET /api/auth/me` + `GET /api/bands/my`.
- `client.ts` 가 401 을 받으면 refresh 한 번 시도 후 원요청을 재시도한다.
- access 토큰은 메모리에만 둔다 (새로고침하면 refresh 쿠키로 다시 복구).
- **초대 코드 표시**: 조회 전용 API 가 없어서, 멤버 화면에서 밴드장이 "발급/재발급"을 눌러야 코드가 보인다.
- **owner/member 판정**: `GET /api/bands/my` 응답에 아직 role 이 없어서, 현재 밴드의 멤버 목록에서
  내 역할을 찾아 판정한다. 백엔드가 my 응답에 role 을 추가하면 밴드 전환 시트 라벨도 정확해진다.

## 현재 상태 — 목업 이식 완료 (5개 화면 전부)

- [x] 앱 셸: 데스크탑 사이드바 / 모바일 상단바 + 하단 탭바 (900px 분기)
- [x] 밴드 전환 시트, 새 밴드 만들기, 로그인 모달, 비회원 배너
- [x] `DevRoleBar` — 백엔드 전 owner/member/guest 프리뷰 (dev 빌드 전용)
- [x] 홈 — 배너·통계·다가오는 일정·최근 곡·최근 영상
- [x] 곡 — 위시/합주 탭, 득표순·최신순 정렬, 투표(1인1표 토글),
      상세 펼침(파트 배정·메모·참고영상), 밴드장 승격, 곡 추가 모달(검색/직접입력 + 세션 구성)
- [x] 일정 — 월간 캘린더(이전/다음 달 이동), 일정 리스트, 상세 패널
      (내 출결 선택, 연결된 영상, 멤버 출결 현황). 시드는 2026-08 만 — 다른 달은 빈 캘린더.
- [x] 영상 — 전체/합주/공연 필터, 카드 그리드(공개범위 배지·일정 연결 표시),
      영상 첨부 모달. 비회원은 '멤버만' 영상 숨김(기획서 8.7).
- [x] 멤버 — 멤버 리스트(역할 배지), 밴드장 추방, 초대 코드 표시·재발급·링크 복사

### 목업과 다르게 / 스펙과 다르게

- 파트 배정 권한: **로그인한 멤버 누구나**(목업 동작). 비회원은 읽기 전용.
  기획서 8.5 `PUT .../assign = 밴드장` 과 다름 → 백엔드 시 정책 재확인.
- 곡 투표·승격·파트 배정, 일정 출결, 영상 첨부는 아직 클라 상태 목.
  `AppContext` 에 상태/액션만 있고 영속성 없음(새로고침하면 초기화).
- 곡/일정/영상 mock 시드는 `bandId: 'b1'` 이라 실제(숫자 id) 밴드에서는 비어 보인다 — 정상.

## 남은 연동 (백엔드 Phase 4-4~ 이후)

- 곡: `GET/POST /api/bands/{id}/songs`, 투표, `PATCH .../confirm`, 파트 배정 → `mock` 제거
- 일정: `GET/POST /api/bands/{id}/schedules`, 출결
- 영상: `GET/POST /api/bands/{id}/media`, visibility
- 로고/배너 업로드: `bands.ts` 에 `uploadLogo/uploadBanner` 는 있으나 아직 호출하는 UI 없음
