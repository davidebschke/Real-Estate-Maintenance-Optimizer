CREATE TABLE properties (
    id        VARCHAR(36)      PRIMARY KEY,
    name      VARCHAR(50)      NOT NULL,
    address   VARCHAR(200)     NOT NULL,
    icon      VARCHAR(50)      NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    version   BIGINT           NOT NULL
);

CREATE TABLE appointments (
    id                         VARCHAR(36)    PRIMARY KEY,
    series_id                  VARCHAR(36),
    title                      VARCHAR(50)    NOT NULL,
    property_id                VARCHAR(36)    NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    description                VARCHAR(2000)  NOT NULL,
    start_at                   TIMESTAMP(6)   NOT NULL,
    end_at                     TIMESTAMP(6)   NOT NULL,
    actual_end                 TIMESTAMP(6),
    locked                     BOOLEAN        NOT NULL,
    recurring                  BOOLEAN        NOT NULL,
    recurrence_interval_months INTEGER,
    materials                  VARCHAR(255)[] NOT NULL,
    version                    BIGINT         NOT NULL
);

CREATE INDEX appointments_series_id_idx ON appointments (series_id);
CREATE INDEX appointments_property_id_idx ON appointments (property_id);
CREATE INDEX appointments_start_at_idx ON appointments (start_at);

CREATE TABLE appointment_history_entries (
    appointment_id VARCHAR(36)              NOT NULL REFERENCES appointments (id) ON DELETE CASCADE,
    sort_order     INTEGER                  NOT NULL,
    occurred_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    type           VARCHAR(20)              NOT NULL,
    message_args   VARCHAR(255)[]           NOT NULL,
    PRIMARY KEY (appointment_id, sort_order)
);

-- Defense in depth for Supabase: no policies means no access for its API roles, while the owning backend role bypasses RLS
ALTER TABLE properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment_history_entries ENABLE ROW LEVEL SECURITY;
