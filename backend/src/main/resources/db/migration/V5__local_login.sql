-- 이메일/비밀번호 로그인 추가. 기존 사용자는 전부 카카오.
alter table users
    alter column kakao_id drop not null;

alter table users
    add column password_hash varchar(100);

alter table users
    add column provider varchar(20) not null default 'KAKAO';

-- 이메일은 있을 때만 유일 (Postgres 는 NULL 중복 허용)
alter table users
    add constraint uq_users_email unique (email);
