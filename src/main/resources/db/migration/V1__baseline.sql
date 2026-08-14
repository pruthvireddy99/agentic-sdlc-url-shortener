create table short_urls (
    id uuid primary key,
    short_code varchar(32) not null unique,
    original_url varchar(2048) not null,
    created_at timestamptz not null,
    expires_at timestamptz null,
    click_count bigint not null default 0,
    idempotency_key varchar(128) null unique,
    version bigint not null default 0
);

create index idx_short_urls_created_at on short_urls(created_at);

create table click_events (
    id uuid primary key,
    short_code varchar(32) not null,
    occurred_at timestamptz not null,
    user_agent_hash varchar(128) null,
    referrer_domain varchar(255) null
);

create index idx_click_events_code_time on click_events(short_code, occurred_at);

create table orchestration_runs (
    id uuid primary key,
    scenario varchar(32) not null,
    status varchar(32) not null,
    plan_version integer not null,
    requirement text not null,
    context_json text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    stopped_at timestamptz null,
    stop_reason varchar(1000) null
);

create table orchestration_nodes (
    id uuid primary key,
    run_id uuid not null references orchestration_runs(id),
    node_key varchar(100) not null,
    node_type varchar(64) not null,
    status varchar(32) not null,
    dependencies_csv varchar(2000) not null,
    attempt integer not null default 0,
    max_attempts integer not null default 3,
    output_json text null,
    error_message varchar(4000) null,
    started_at timestamptz null,
    completed_at timestamptz null,
    constraint uq_run_node unique (run_id, node_key)
);

create index idx_nodes_run_status on orchestration_nodes(run_id, status);

create table audit_events (
    id uuid primary key,
    run_id uuid not null references orchestration_runs(id),
    event_type varchar(64) not null,
    actor varchar(128) not null,
    node_key varchar(100) null,
    event_at timestamptz not null,
    payload_json text null
);

create index idx_audit_run_time on audit_events(run_id, event_at);
