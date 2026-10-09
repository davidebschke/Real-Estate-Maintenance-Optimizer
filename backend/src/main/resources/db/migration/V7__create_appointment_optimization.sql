-- How many AI optimization runs a demo account may still start; NULL means unlimited (regular accounts)
ALTER TABLE users ADD COLUMN remaining_ai_optimizations INTEGER;

-- Start a recurring occurrence was planned for before the first AI optimization moved it, bounding further automatic moves
ALTER TABLE appointments ADD COLUMN recurrence_anchor_start TIMESTAMP(6);

CREATE TABLE optimization_runs (
    id              VARCHAR(36)                 PRIMARY KEY,
    owner_id        VARCHAR(36)                 NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    model           VARCHAR(100)                NOT NULL,
    candidate_count INTEGER                     NOT NULL,
    version         BIGINT                      NOT NULL
);

CREATE INDEX optimization_runs_owner_id_idx ON optimization_runs (owner_id);

-- Accepted proposals outlive their appointment (appointment_id is set to NULL) so the savings statistics stay complete
CREATE TABLE optimization_proposals (
    id                     VARCHAR(36)                 PRIMARY KEY,
    run_id                 VARCHAR(36)                 NOT NULL REFERENCES optimization_runs (id) ON DELETE CASCADE,
    owner_id               VARCHAR(36)                 NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    appointment_id         VARCHAR(36)                 REFERENCES appointments (id) ON DELETE SET NULL,
    appointment_title      VARCHAR(50)                 NOT NULL,
    property_name          VARCHAR(50)                 NOT NULL,
    recurring              BOOLEAN                     NOT NULL,
    original_start         TIMESTAMP(6)                NOT NULL,
    original_end           TIMESTAMP(6)                NOT NULL,
    proposed_start         TIMESTAMP(6)                NOT NULL,
    proposed_end           TIMESTAMP(6)                NOT NULL,
    saved_distance_meters  DOUBLE PRECISION            NOT NULL,
    saved_duration_seconds DOUBLE PRECISION            NOT NULL,
    reason                 VARCHAR(500)                NOT NULL,
    status                 VARCHAR(20)                 NOT NULL,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    decided_at             TIMESTAMP(6) WITH TIME ZONE,
    version                BIGINT                      NOT NULL
);

CREATE INDEX optimization_proposals_owner_id_status_idx ON optimization_proposals (owner_id, status);
CREATE INDEX optimization_proposals_appointment_id_idx ON optimization_proposals (appointment_id);

-- Defense in depth for Supabase, same reasoning as for the tables of V1
ALTER TABLE optimization_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE optimization_proposals ENABLE ROW LEVEL SECURITY;
