-- One confirmed rent payment of a tenant; the period is the month the rent was paid for, which may differ from the payment date
CREATE TABLE rent_payments (
    id           VARCHAR(36)   PRIMARY KEY,
    tenant_id    VARCHAR(36)   NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    payment_date DATE          NOT NULL,
    amount       NUMERIC(9, 2) NOT NULL,
    reference    VARCHAR(200)  NOT NULL,
    period_year  SMALLINT      NOT NULL,
    period_month SMALLINT      NOT NULL,
    version      BIGINT        NOT NULL,
    CONSTRAINT rent_payments_period_month_check CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT rent_payments_amount_check CHECK (amount > 0)
);

CREATE INDEX rent_payments_tenant_id_idx ON rent_payments (tenant_id);

-- Defense in depth for Supabase, same reasoning as for the tables of V1
ALTER TABLE rent_payments ENABLE ROW LEVEL SECURITY;
