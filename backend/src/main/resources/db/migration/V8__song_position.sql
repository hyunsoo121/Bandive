-- 곡의 수동 정렬 순서. (band, status, folder 또는 미분류) 그룹 안에서의 위치.
alter table songs add column position integer not null default 0;

-- 기존 곡들: 그룹별로 생성순(id) 대로 0,1,2… 부여
with ordered as (
    select id,
           row_number() over (
               partition by band_id, status, folder_id
               order by id
           ) - 1 as pos
    from songs
)
update songs s
set position = ordered.pos
from ordered
where ordered.id = s.id;
