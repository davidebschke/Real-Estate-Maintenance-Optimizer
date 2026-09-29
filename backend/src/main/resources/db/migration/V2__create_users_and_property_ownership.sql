CREATE TABLE users (
    id                              VARCHAR(36)                 PRIMARY KEY,
    username                        VARCHAR(50)                 NOT NULL UNIQUE CHECK (username = LOWER(username)),
    password_hash                   VARCHAR(100),
    display_name                    VARCHAR(100)                NOT NULL,
    demo_account                    BOOLEAN                     NOT NULL,
    created_at                      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at                      TIMESTAMP(6) WITH TIME ZONE,
    remaining_property_creations    INTEGER,
    remaining_appointment_creations INTEGER,
    sessions_valid_from             TIMESTAMP(6) WITH TIME ZONE,
    version                         BIGINT                      NOT NULL
);

CREATE INDEX users_demo_account_expires_at_idx ON users (expires_at) WHERE demo_account;

-- The productive account; its password hash stays NULL (login impossible) until the backend sets it on startup from REMO_AUTH_INITIAL_USER_PASSWORD
INSERT INTO users (id, username, password_hash, display_name, demo_account, created_at, expires_at,
                   remaining_property_creations, remaining_appointment_creations, version)
VALUES ('00000000-0000-0000-0000-000000000001', 'debschke', NULL, 'David Ebschke', FALSE, NOW(), NULL, NULL, NULL, 0);

-- Every property (and through it every appointment) belongs to exactly one account; data created before accounts existed goes to debschke
ALTER TABLE properties ADD COLUMN owner_id VARCHAR(36);
UPDATE properties SET owner_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE properties ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE properties
    ADD CONSTRAINT properties_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX properties_owner_id_idx ON properties (owner_id);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
