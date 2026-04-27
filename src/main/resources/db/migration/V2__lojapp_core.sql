CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    brand_id BIGINT REFERENCES brands (id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    ean VARCHAR(30),
    ncm VARCHAR(12),
    sku VARCHAR(64),
    cost_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    sale_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    minimum_stock NUMERIC(19, 3) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE nfe_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    nfe_number VARCHAR(50) NOT NULL,
    supplier_name VARCHAR(255),
    access_key VARCHAR(80),
    raw_xml TEXT NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE nfe_items (
    id BIGSERIAL PRIMARY KEY,
    nfe_entry_id BIGINT NOT NULL REFERENCES nfe_entries (id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products (id) ON DELETE SET NULL,
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    movement_type VARCHAR(20) NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    source VARCHAR(40) NOT NULL,
    source_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_balances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity NUMERIC(19, 3) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, product_id)
);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    sold_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_user ON products (user_id);
CREATE INDEX idx_inventory_movements_user_product ON inventory_movements (user_id, product_id);
CREATE INDEX idx_sales_user_sold_at ON sales (user_id, sold_at);
