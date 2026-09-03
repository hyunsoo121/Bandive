-- SEARCH 로 추가한 곡의 앨범 커버 (외부 이미지 URL). 직접 입력 곡은 null.
alter table songs
    add column artwork_url varchar(500);
