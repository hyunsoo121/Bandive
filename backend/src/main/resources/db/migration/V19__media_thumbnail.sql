-- 구글 포토 공유 링크처럼 URL 만으로 썸네일을 계산할 수 없는 플랫폼을 위해, 등록/수정 시점에
-- 한 번 fetch 해둔 썸네일을 저장한다. null 이면 기존처럼 유튜브·드라이브를 즉석 계산(MediaThumbnail).
alter table media
    add column thumbnail_url varchar(500);
