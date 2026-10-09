CREATE TABLE apartments (
    id                 VARCHAR(36)   PRIMARY KEY,
    property_id        VARCHAR(36)   NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    floor              INTEGER       NOT NULL,
    area_square_meters NUMERIC(7, 2) NOT NULL,
    total_rent         NUMERIC(9, 2) NOT NULL,
    cold_rent          NUMERIC(9, 2) NOT NULL,
    additional_costs   NUMERIC(9, 2) NOT NULL,
    version            BIGINT        NOT NULL
);

CREATE INDEX apartments_property_id_idx ON apartments (property_id);

CREATE TABLE tenants (
    id           VARCHAR(36) PRIMARY KEY,
    apartment_id VARCHAR(36) NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    first_name   VARCHAR(50) NOT NULL,
    last_name    VARCHAR(50) NOT NULL,
    version      BIGINT      NOT NULL
);

CREATE INDEX tenants_apartment_id_idx ON tenants (apartment_id);

-- Defense in depth for Supabase, same reasoning as for the tables of V1
ALTER TABLE apartments ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
