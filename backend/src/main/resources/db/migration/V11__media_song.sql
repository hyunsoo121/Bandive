-- 영상을 합주곡(CONFIRMED 곡)과 선택적으로 연결. 곡 삭제 시 연결만 끊긴다.
alter table media add column song_id bigint references songs (id) on delete set null;
create index idx_media_song on media (song_id);
