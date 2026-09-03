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
  api/          client.ts (fetch 래퍼 + 토큰/401 재시도), auth/bands/invites/members/
                songs/schedules/media.ts, types.ts (백엔드 DTO), mappers.ts (DTO → 도메인 타입)
  store/        AppContext.tsx (세션·현재밴드·멤버·초대·곡·일정·영상 전부 API 연동.
                현재 밴드가 바뀌면 곡/일정/영상을 그 밴드 것으로 재요청)
  hooks/        useGuard.ts (비회원 액션 → 로그인 모달)
  lib/          nav.ts (탭/사이드바 정의), songs.ts (세션 슬롯 계산),
                schedule.ts (enum↔한글 변환, 날짜 파생값 toUi, nextSchedule)
  components/    AppLayout(셸), BandSwitcher, LoginModal, CreateBandModal, Modal, Fab,
                AddSongModal, AddMediaModal, AddScheduleModal, DevRoleBar, ...
  pages/        HomePage, SongsPage, SchedulePage, MediaPage, MembersPage,
                SystemPages.tsx (HomeRedirect / OAuthSuccess / OAuthFailure / InviteJoin)
```

## 라우팅

- `/` → `HomeRedirect`: 세션 복구 후 내 첫 밴드로. 밴드 없으면 로그인/생성 안내
- `/oauth/success` · `/oauth/failure` — 카카오 로그인 복귀 지점
- `/invite/:code` — 초대 링크. 로그인돼 있으면 바로 가입 후 해당 밴드로
- `/bands/:bandId` (AppLayout) 하위: index=홈, `songs` `schedule` `media` `members`

## API 연동 현황

| 도메인                                            | 상태                                      |
| ------------------------------------------------- | ----------------------------------------- |
| 인증 (카카오 OAuth2 → refresh 쿠키 → access 토큰) | ✅ 연동                                   |
| 밴드 (내 밴드 목록 / 상세 / 생성)                 | ✅ 연동                                   |
| 멤버 (목록 / 추방)                                | ✅ 연동                                   |
| 초대 (코드 발급·재발급 / 코드로 가입)             | ✅ 연동                                   |
| 곡 (목록/추가/검색/투표/승격/파트배정/삭제)       | ✅ 연동                                   |
| 일정 (목록/등록/삭제/출결)                        | ✅ 연동                                   |
| 영상 (목록/등록/공개범위/삭제)                    | ✅ 연동                                   |
| 로고/배너 업로드                                  | ⏳ `bands.ts` 에 함수만 있고 호출 UI 없음 |

- **인증 플로우**: `login()` → `{API}/oauth2/authorization/kakao` 로 페이지 이동 → 카카오 →
  백엔드가 refresh 를 httpOnly 쿠키로 심고 `/oauth/success` 로 리다이렉트 →
  앱 부팅 시 `POST /api/auth/refresh` 로 access 토큰 획득(메모리 보관) → `GET /api/auth/me` + `GET /api/bands/my`.
- `client.ts` 가 401 을 받으면 refresh 한 번 시도 후 원요청을 재시도한다.
- access 토큰은 메모리에만 둔다 (새로고침하면 refresh 쿠키로 다시 복구).
- **초대 코드 표시**: 조회 전용 API 가 없어서, 멤버 화면에서 밴드장이 "발급/재발급"을 눌러야 코드가 보인다.
- **owner/member 판정**: `GET /api/bands/my` 응답에 아직 role 이 없어서, 현재 밴드의 멤버 목록에서
  내 역할을 찾아 판정한다. 백엔드가 my 응답에 role 을 추가하면 밴드 전환 시트 라벨도 정확해진다.
- **영상 제목 없음**: 백엔드 `Media` 는 URL·종류·공개범위만 저장(제목 필드 없음). 카드/상세는
  URL 을 짧게 줄인 문자열을 표시하고 클릭하면 원본으로 연다 (`mappers.toMedia` 의 `prettyUrl`).
- **일정 제목 없음**: `Schedule` 은 종류·일시·장소만. 리스트/상세 헤딩은 `장소`(없으면 종류 라벨)를 쓴다.
  캘린더는 "가장 가까운 다가오는 일정"이 있는 달로 초점을 맞춘다 (`lib/schedule.nextSchedule`).
- **곡 검색은 스텁**: `GET /api/songs/search` 는 쿼리를 그대로 3건으로 돌려주는 백엔드 스텁
  (`StubMusicSearchService`, Phase 5 에서 실 API). 검색 결과를 **골라야** `SEARCH`(+externalTrackId)로,
  직접 입력하면 `MANUAL` 로 등록된다.
- **파트 배정**: 슬롯키(`"기타#2"`)를 `Song.parts` 에서 `partId` 로, 멤버 이름을 `userId` 로 되짚어
  `PUT .../parts/{partId}/assign` 을 호출한다. `CONFIRMED` 곡에서만 가능(그 외 409).

## 현재 상태 — 목업 이식 완료 (5개 화면 전부)

- [x] 앱 셸: 데스크탑 사이드바 / 모바일 상단바 + 하단 탭바 (900px 분기)
- [x] 밴드 전환 시트, 새 밴드 만들기, 로그인 모달, 비회원 배너
- [x] `DevRoleBar` — 백엔드 전 owner/member/guest 프리뷰 (dev 빌드 전용)
- [x] 홈 — 배너·통계·다가오는 일정·최근 곡·최근 영상
- [x] 곡 — 위시/합주 탭, 득표순·최신순 정렬, 투표(1인1표 토글),
      상세 펼침(파트 배정·메모·참고영상), 밴드장 승격, 곡 추가 모달(검색/직접입력 + 세션 구성)
- [x] 일정 — 월간 캘린더(이전/다음 달 이동, 다가오는 일정 달로 초점), 일정 리스트, 상세 패널
      (내 출결 선택, 연결된 영상, 멤버 출결 현황), 일정 등록 모달(종류/일시/장소)
- [x] 영상 — 전체/합주/공연 필터, 카드 그리드(공개범위 배지·일정 연결 표시),
      영상 첨부 모달. 비회원은 '멤버만' 영상 숨김(기획서 8.7 — 백엔드가 GET 응답에서 제외).
- [x] 멤버 — 멤버 리스트(역할 배지), 밴드장 추방, 초대 코드 표시·재발급·링크 복사

### 목업과 다르게 / 스펙과 다르게

- 파트 배정 권한: **로그인한 멤버 누구나**(ADR-007). 비회원은 읽기 전용.
- 곡 검색 결과·영상/일정 제목·캘린더 초점 달은 위 "API 연동 현황" 노트 참고.
- 곡/일정/영상 액션은 API 호출 후 응답으로 상태를 갱신한다(낙관적 갱신 아님).
  vote/confirm/assign/attendance 실패는 `console.error` 만; add 계열은 모달에서 에러 문구 표시.

## 남은 연동

- 로고/배너 업로드: `bands.ts` 에 `uploadLogo/uploadBanner` 는 있으나 아직 호출하는 UI 없음
  (홈 배너 "배너 업로드" 버튼은 현재 no-op)
- 곡 삭제 / 일정 삭제 액션(`removeSong` / `removeSchedule` / `removeMedia`)은 `AppContext` 에
  구현돼 있으나 페이지에 버튼이 아직 없다
