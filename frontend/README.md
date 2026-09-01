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

## 구조

```
src/
  styles/       tokens.css (모더니스트 디자인 시스템), shared.css (버튼/세그/모달 등 프리미티브)
  types.ts      도메인 타입 (ERD 대응)
  mock/         data.ts (시드), selectors.ts (일정 조회 헬퍼)
  store/        AppContext.tsx (세션·현재밴드·모달 + songs/media/members/inviteCodes 상태와 액션)
  hooks/        useGuard.ts (비회원 액션 → 로그인 모달)
  lib/          nav.ts (탭/사이드바 정의), songs.ts (세션 슬롯 계산)
  components/    AppLayout(셸), BandSwitcher, LoginModal, CreateBandModal, Modal, Fab,
                AddSongModal, AddMediaModal, DevRoleBar, ...
  pages/        HomePage, SongsPage, SchedulePage, MediaPage, MembersPage
```

## 라우팅

- `/` → `/bands/:bandId` 로 리다이렉트
- `/bands/:bandId` (AppLayout) 하위: index=홈, `songs` `schedule` `media` `members`

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
- 일정 출결·멤버 추방·초대 코드는 아직 클라 상태 목. `AppContext` 에 상태/액션만 있고
  영속성 없음(새로고침하면 초기화).

## 백엔드 연동 시 교체 지점

- `mock/data.ts`, `mock/selectors.ts` → `src/api/*` 호출로 대체
- `AppContext.login/logout` → 카카오 OAuth2 리다이렉트 플로우
- `AppContext.bands` → `GET /api/bands/my`
- `useGuard` 는 그대로 (401 처리와 병행)
- `vite.config.ts` 의 `/api` 프록시 타깃 확인
