create table rooms (
    id uuid primary key,
    code varchar(6) not null unique,
    name varchar(80) not null,
    status varchar(20) not null,
    is_locked boolean not null default false,
    max_players integer not null check (max_players between 2 and 300),
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null default now(),
    closed_at timestamptz
);

create table room_members (
    id uuid primary key,
    room_id uuid not null references rooms(id) on delete cascade,
    display_name varchar(40) not null,
    role varchar(16) not null,
    connection_status varchar(20) not null,
    joined_at timestamptz not null,
    left_at timestamptz
);

create unique index ux_room_member_display_name on room_members (room_id, lower(display_name));
create index ix_room_members_room on room_members (room_id);

create table physical_card_imports (
    id uuid primary key,
    file_name varchar(255) not null,
    file_checksum varchar(64) not null,
    import_version varchar(40) not null,
    total_rows integer not null,
    valid_rows integer not null,
    invalid_rows integer not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    completed_at timestamptz
);

create unique index ux_physical_card_completed_import
    on physical_card_imports (file_checksum) where status = 'COMPLETED';

create table physical_cards (
    id uuid primary key,
    external_id varchar(32) not null unique,
    numbers_json jsonb not null,
    grid_json jsonb not null,
    structure_hash varchar(64) not null unique,
    source varchar(24) not null,
    source_reference varchar(255),
    layout_source varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);
