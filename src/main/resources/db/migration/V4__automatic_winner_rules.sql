alter table games add column automatic_bingo_detection_enabled boolean not null default false;
alter table games add column stop_on_bingo_enabled boolean not null default true;
alter table games add column winner_announcement_enabled boolean not null default true;
