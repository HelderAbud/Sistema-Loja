CREATE TABLE sellers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sellers_user_active_sort ON sellers (user_id, active, sort_order, id);

CREATE TABLE commission_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    brand_id BIGINT REFERENCES brands (id) ON DELETE CASCADE,
    percent NUMERIC(7, 4) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_commission_rules_percent CHECK (percent >= 0 AND percent <= 100)
);

CREATE INDEX idx_commission_rules_user_brand_from
    ON commission_rules (user_id, brand_id, valid_from DESC);

CREATE TABLE seller_queue_state (
    cash_session_id BIGINT PRIMARY KEY REFERENCES cash_sessions (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_assigned_seller_id BIGINT REFERENCES sellers (id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE sales
    ADD COLUMN seller_id BIGINT REFERENCES sellers (id) ON DELETE SET NULL;

CREATE INDEX idx_sales_user_seller_sold_at ON sales (user_id, seller_id, sold_at DESC);

CREATE TABLE commission_accruals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sale_id BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    sale_item_id BIGINT REFERENCES sale_items (id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES sellers (id) ON DELETE RESTRICT,
    commission_rule_id BIGINT REFERENCES commission_rules (id) ON DELETE SET NULL,
    brand_id BIGINT REFERENCES brands (id) ON DELETE SET NULL,
    base_amount NUMERIC(19, 2) NOT NULL,
    percent NUMERIC(7, 4) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_commission_accruals_user_seller_created
    ON commission_accruals (user_id, seller_id, created_at DESC);
