-- 탐색/팔로우 테스트용 더미 데이터.
-- 실행:  docker exec -i bandive-postgres psql -U bandive -d bandive < backend/scripts/seed-explore-demo.sql
-- 되돌리기:  아래 "-- ROLLBACK" 블록 참고.
--
-- 만드는 것:
--   * 데모 유저 3명 (데모기타, 데모드럼, 데모팬)
--   * 데모 밴드 2개 — 같은 곡("좋은 날")의 공개 유튜브 합주 영상.
--     "데모 밴드 · 서울" = 전체공개 (탐색 곡 탭에 노출), "데모 밴드 · 부산" = 팔로워 공개
--     (탐색 밴드 탭에서 열면 "팔로우 요청" 버튼 확인 가능)
--   * 내 가장 최근 밴드를 FOLLOWERS(팔로워 공개)로 바꾸고, 데모 유저 2명의 팔로우 요청(PENDING) 추가
--     → 그 밴드 멤버 페이지에 "팔로우 요청 2건"이 뜬다

begin;

-- ── 데모 유저 ──
insert into users (kakao_id, nickname, provider, created_at, updated_at)
select v.kakao_id, v.nickname, 'KAKAO', now(), now()
from (values ('demo-guitar', '데모기타'), ('demo-drum', '데모드럼'), ('demo-fan', '데모팬')) as v(kakao_id, nickname)
where not exists (select 1 from users u where u.kakao_id = v.kakao_id);

-- ── 데모 밴드 2개 (전체공개) ──
insert into bands (name, visibility, description, created_at, updated_at)
select v.name, 'PUBLIC', v.description, now(), now()
from (values ('데모 밴드 · 서울', '탐색 테스트용'), ('데모 밴드 · 부산', '탐색 테스트용')) as v(name, description)
where not exists (select 1 from bands b where b.name = v.name);

-- 소유자(OWNER) 등록: 서울=데모기타, 부산=데모드럼
insert into band_members (band_id, user_id, role, joined_at, is_leader, created_at, updated_at)
select b.id, u.id, 'OWNER', now(), false, now(), now()
from bands b
  join users u on (b.name = '데모 밴드 · 서울' and u.kakao_id = 'demo-guitar')
              or (b.name = '데모 밴드 · 부산' and u.kakao_id = 'demo-drum')
where not exists (select 1 from band_members m where m.band_id = b.id and m.user_id = u.id);

-- 같은 트랙("좋은 날 / 아이유")을 두 밴드에 CONFIRMED 곡으로
insert into songs (band_id, title, artist, status, source_type, external_track_id, artwork_url, added_by, position,
                   created_at, updated_at)
select b.id, '좋은 날', '아이유', 'CONFIRMED', 'SEARCH', 'demo-track-goodday',
       'https://is1-ssl.mzstatic.com/image/thumb/Music/v4/1e/2a/3c/goodday/600x600bb.jpg',
       m.user_id, 0, now(), now()
from bands b
  join band_members m on m.band_id = b.id and m.role = 'OWNER'
where b.name in ('데모 밴드 · 서울', '데모 밴드 · 부산')
  and not exists (select 1 from songs s where s.band_id = b.id and s.external_track_id = 'demo-track-goodday');

-- 각 밴드에 그 곡의 공개(LINK_PUBLIC) 유튜브 영상 1개
insert into media (band_id, song_id, type, external_url, platform, visibility, title, uploaded_by, created_at, updated_at)
select s.band_id, s.id, 'REHEARSAL',
       case b.name when '데모 밴드 · 서울' then 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
                   else 'https://youtu.be/9bZkp7q19f0' end,
       'YOUTUBE', 'LINK_PUBLIC', '좋은 날 합주',
       m.user_id, now(), now()
from songs s
  join bands b on b.id = s.band_id
  join band_members m on m.band_id = b.id and m.role = 'OWNER'
where s.external_track_id = 'demo-track-goodday'
  and not exists (select 1 from media md where md.song_id = s.id);

-- "데모 밴드 · 부산" 은 팔로워 공개로 (탐색에서 팔로우 요청 버튼 테스트용)
update bands set visibility = 'FOLLOWERS', updated_at = now() where name = '데모 밴드 · 부산';

-- ── 내 최근 밴드를 FOLLOWERS 로 + 데모 유저 팔로우 요청 ──
with target as (
  select id from bands
  where name not like '데모 밴드%'
  order by id desc
  limit 1
)
update bands set visibility = 'FOLLOWERS', updated_at = now()
where id in (select id from target);

insert into band_followers (band_id, user_id, status, created_at, updated_at)
select t.id, u.id, 'PENDING', now(), now()
from (select id from bands where name not like '데모 밴드%' order by id desc limit 1) t
  join users u on u.kakao_id in ('demo-guitar', 'demo-drum')
where not exists (select 1 from band_followers f where f.band_id = t.id and f.user_id = u.id);

commit;

-- 결과 확인
select '데모 밴드' as kind, id, name, visibility from bands where name like '데모 밴드%'
union all
select 'FOLLOWERS 로 바뀐 내 밴드', id, name, visibility from bands where visibility = 'FOLLOWERS' and name not like '데모 밴드%';
select f.status, u.nickname, b.name as band
from band_followers f join users u on u.id = f.user_id join bands b on b.id = f.band_id;

-- ─────────────────────────────────────────────────────────────
-- ROLLBACK (더미 데이터 제거) — 필요하면 아래를 psql 로 실행:
--   delete from media where song_id in (select id from songs where external_track_id = 'demo-track-goodday');
--   delete from songs where external_track_id = 'demo-track-goodday';
--   delete from band_members where band_id in (select id from bands where name like '데모 밴드%');
--   delete from bands where name like '데모 밴드%';
--   delete from band_followers where user_id in (select id from users where kakao_id like 'demo-%');
--   delete from users where kakao_id like 'demo-%';
--   -- (FOLLOWERS 로 바꾼 내 밴드는 앱의 밴드 설정에서 원하는 공개범위로 되돌리면 됨)
