-- 영상 고정(관리자, 여러 개 가능). null 이면 고정 아님, 값이 있으면 고정한 시각 —
-- 고정된 것끼리는 이 시각 내림차순(최근에 고정한 게 먼저)으로 정렬한다.
alter table media
    add column pinned_at timestamptz;
