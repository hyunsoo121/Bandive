-- 일정 제목. 그동안은 장소 텍스트가 사실상 제목 역할을 했는데, 장소와 분리된 진짜 제목을 붙일 수 있게.
-- 선택 입력이라 nullable — 없으면 프론트가 기존처럼 종류(합주/공연) 라벨로 표시.
alter table schedules
    add column title varchar(100);
