-- 게스트 이름은 밴드 안에서 유일해야 한다 (같은 이름 재등록 방지).
alter table band_guests
    add constraint uq_band_guest_name unique (band_id, name);

-- 게스트가 그 일정에서 맡는 세션(악기 또는 "관객"). 실멤버 출결에는 쓰지 않는다.
alter table attendances
    add column session varchar(30);
