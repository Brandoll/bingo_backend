create table games (
    id uuid primary key,
    room_id uuid not null references rooms(id) on delete cascade,
    round_number integer not null check (round_number > 0),
    status varchar(24) not null,
    current_draw_order integer not null default 0,
    automatic_draw_enabled boolean not null default false,
    automatic_draw_interval_seconds integer not null default 8 check (automatic_draw_interval_seconds between 3 and 60),
    next_automatic_draw_at timestamptz,
    line_enabled boolean not null default true,
    double_line_enabled boolean not null default true,
    bingo_enabled boolean not null default true,
    ranking_public boolean not null default true,
    started_at timestamptz not null,
    paused_at timestamptz,
    ended_at timestamptz,
    version bigint not null default 0,
    unique (room_id, round_number)
);

create index ix_games_room on games (room_id, round_number desc);
create index ix_games_automatic on games (next_automatic_draw_at)
    where automatic_draw_enabled = true and status = 'RUNNING';

create table drawn_numbers (
    id uuid primary key,
    game_id uuid not null references games(id) on delete cascade,
    number integer not null check (number between 1 and 90),
    draw_order integer not null check (draw_order > 0),
    drawn_by uuid references room_members(id),
    drawn_at timestamptz not null,
    unique (game_id, number),
    unique (game_id, draw_order)
);

create index ix_drawn_numbers_game on drawn_numbers (game_id, draw_order);

create table game_cards (
    id uuid primary key,
    game_id uuid not null references games(id) on delete cascade,
    member_id uuid references room_members(id) on delete cascade,
    physical_card_id uuid references physical_cards(id),
    card_type varchar(16) not null,
    display_name varchar(40) not null,
    external_code varchar(32) not null,
    grid_json jsonb not null,
    is_active boolean not null default true,
    assigned_at timestamptz not null,
    unique (game_id, external_code),
    unique (game_id, physical_card_id)
);

create index ix_game_cards_member on game_cards (game_id, member_id);

create table card_marks (
    id uuid primary key,
    game_card_id uuid not null references game_cards(id) on delete cascade,
    member_id uuid not null references room_members(id) on delete cascade,
    number integer not null check (number between 1 and 90),
    marked_at timestamptz not null,
    unique (game_card_id, number)
);

create table prize_claims (
    id uuid primary key,
    game_id uuid not null references games(id) on delete cascade,
    game_card_id uuid not null references game_cards(id) on delete cascade,
    member_id uuid references room_members(id),
    prize_type varchar(20) not null,
    status varchar(20) not null,
    claimed_at timestamptz not null,
    validated_at timestamptz,
    validated_by uuid references room_members(id),
    rejection_reason varchar(240)
);

create index ix_prize_claims_pending on prize_claims (game_id, status, claimed_at);

create table game_events (
    id uuid primary key,
    game_id uuid not null references games(id) on delete cascade,
    event_type varchar(40) not null,
    actor_member_id uuid references room_members(id),
    payload_json jsonb,
    occurred_at timestamptz not null
);

create index ix_game_events_game on game_events (game_id, occurred_at);
