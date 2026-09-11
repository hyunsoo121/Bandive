-- 밴드 공개범위. PRIVATE=멤버만 / FOLLOWERS=멤버+승인된 팔로워 / PUBLIC=누구나.
-- 기존 밴드는 지금 동작(누구나 열람)을 유지하도록 PUBLIC.
alter table bands
    add column visibility varchar(20) not null default 'PUBLIC';
