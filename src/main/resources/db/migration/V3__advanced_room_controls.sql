alter table rooms add column cards_per_player integer not null default 1
    check (cards_per_player between 1 and 4);
alter table rooms add column allow_late_join boolean not null default true;
alter table rooms add column hide_participant_names boolean not null default false;

create index ix_room_members_role on room_members (room_id, role);
