-- 밴드 소개 문구. 기존 행이 있을 수 있으므로 nullable.
alter table bands
    add column description varchar(500);
