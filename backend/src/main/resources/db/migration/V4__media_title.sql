-- 영상에 사용자가 직접 붙이는 제목. 선택 입력이라 nullable.
alter table media
    add column title varchar(200);
