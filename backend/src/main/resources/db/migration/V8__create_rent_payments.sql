-- First day of the month from which a property's rent payments are checked; NULL means the rent payment check is not activated
ALTER TABLE properties ADD COLUMN rent_tracking_start DATE;

-- First day of the month an apartment was created in, so a later created apartment is not judged for earlier months; NULL for apartments of older data
ALTER TABLE apartments ADD COLUMN rent_tracking_from DATE;

-- The rent an apartment had before an increase or decrease took effect in the given month, so earlier months keep being judged by it
CREATE TABLE apartment_rent_changes (
    id              VARCHAR(36)   PRIMARY KEY,
    apartment_id    VARCHAR(36)   NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    effective_month DATE          NOT NULL,
    previous_rent   NUMERIC(9, 2) NOT NULL,
    version         BIGINT        NOT NULL,
    CONSTRAINT apartment_rent_changes_apartment_month_key UNIQUE (apartment_id, effective_month)
);

-- One confirmed rent payment of an apartment; the period is the month the rent was paid for, which may differ from the payment date.
-- A payment keeps the apartment when its tenant is deleted and then carries the tenant's name in former_tenant_name
CREATE TABLE rent_payments (
    id                 VARCHAR(36)   PRIMARY KEY,
    apartment_id       VARCHAR(36)   NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    tenant_id          VARCHAR(36)   REFERENCES tenants (id) ON DELETE SET NULL,
    former_tenant_name VARCHAR(102),
    payment_date       DATE          NOT NULL,
    amount             NUMERIC(9, 2) NOT NULL,
    reference          VARCHAR(200)  NOT NULL,
    period_year        SMALLINT      NOT NULL,
    period_month       SMALLINT      NOT NULL,
    version            BIGINT        NOT NULL,
    CONSTRAINT rent_payments_period_month_check CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT rent_payments_amount_check CHECK (amount > 0)
);

CREATE INDEX rent_payments_apartment_id_idx ON rent_payments (apartment_id);
CREATE INDEX rent_payments_tenant_id_idx ON rent_payments (tenant_id);

-- An identical payment is only stored once per apartment, also when two confirmations arrive at the same time
CREATE UNIQUE INDEX rent_payments_identical_payment_idx ON rent_payments (apartment_id, payment_date, amount, reference);

-- Defense in depth for Supabase, same reasoning as for the tables of V1
ALTER TABLE apartment_rent_changes ENABLE ROW LEVEL SECURITY;
ALTER TABLE rent_payments ENABLE ROW LEVEL SECURITY;
