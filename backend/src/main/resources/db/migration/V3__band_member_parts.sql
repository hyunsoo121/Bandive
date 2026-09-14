-- 밴드에서 멤버가 맡은 파트(악기). 한 멤버가 여러 개 가질 수 있어 별도 테이블.
-- 값은 Instrument enum 이름(GUITAR 등)이 기본이지만 그 밖의 자유 문자열도 허용.
create table band_member_parts (
    band_member_id bigint      not null references band_members (id) on delete cascade,
    part           varchar(20) not null,
    primary key (band_member_id, part)
);
