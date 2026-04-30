CREATE TABLE cash_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    opened_by_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    closed_by_user_id BIGINT REFERENCES users (id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    opening_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    expected_amount NUMERIC(19, 2),
    counted_amount NUMERIC(19, 2),
    difference_amount NUMERIC(19, 2),
    difference_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_sessions_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE UNIQUE INDEX uq_cash_sessions_user_open
    ON cash_sessions (user_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_cash_sessions_user_status_opened
    ON cash_sessions (user_id, status, opened_at DESC);

CREATE INDEX idx_cash_sessions_user_closed
    ON cash_sessions (user_id, closed_at DESC);

ALTER TABLE sales
    ADD COLUMN cash_session_id BIGINT REFERENCES cash_sessions (id) ON DELETE SET NULL;

CREATE INDEX idx_sales_user_cash_session_sold_at
    ON sales (user_id, cash_session_id, sold_at DESC);

CREATE TABLE sale_payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sale_id BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    payment_method VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sale_payments_method CHECK (payment_method IN ('CASH', 'CARD', 'PIX')),
    CONSTRAINT ck_sale_payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_sale_payments_user_sale
    ON sale_payments (user_id, sale_id);

CREATE INDEX idx_sale_payments_user_method
    ON sale_payments (user_id, payment_method);
