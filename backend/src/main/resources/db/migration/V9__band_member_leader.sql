-- 밴드 리더(합주 진행·대표). 관리자(OWNER)와 별개. 밴드당 최대 1명.
alter table band_members add column is_leader boolean not null default false;

-- 부분 유니크 인덱스: is_leader = true 인 행이 밴드당 하나만
create unique index uq_band_members_leader on band_members (band_id) where is_leader;
