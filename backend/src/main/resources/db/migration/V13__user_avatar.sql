-- 유저 프로필 사진 (자체 스토리지 URL). 미설정이면 null → 프론트는 이니셜 아바타.
alter table users add column avatar_url varchar(500);
