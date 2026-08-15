CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sale_id BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sale_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_sale_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_sale_items_unit_cost_non_negative CHECK (unit_cost >= 0),
    CONSTRAINT uq_sale_items_sale_product UNIQUE (sale_id, product_id)
);

CREATE INDEX idx_sale_items_user_sale ON sale_items (user_id, sale_id);
CREATE INDEX idx_sale_items_user_product ON sale_items (user_id, product_id);

INSERT INTO sale_items (user_id, sale_id, product_id, quantity, unit_price, unit_cost, created_at)
SELECT s.user_id, s.id, s.product_id, s.quantity, s.unit_price, s.unit_cost, s.sold_at
FROM sales s;
