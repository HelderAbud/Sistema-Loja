-- Base multimarcas: fornecedor formal, coleção, modelo; produto como SKU com variantes opcionais.
-- NFe: identificação fiscal do emitente (CNPJ ou CPF, só dígitos) para dedupe futuro.

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cnpj VARCHAR(14),
    legal_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_suppliers_user_cnpj ON suppliers (user_id, cnpj) WHERE cnpj IS NOT NULL;

CREATE INDEX idx_suppliers_user ON suppliers (user_id);

CREATE TABLE product_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, brand_id, name)
);

CREATE INDEX idx_product_collections_user_brand ON product_collections (user_id, brand_id);

CREATE TABLE product_models (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands (id) ON DELETE CASCADE,
    collection_id BIGINT REFERENCES product_collections (id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, brand_id, name)
);

CREATE INDEX idx_product_models_user_brand ON product_models (user_id, brand_id);

ALTER TABLE products
    ADD COLUMN supplier_id BIGINT REFERENCES suppliers (id) ON DELETE SET NULL,
    ADD COLUMN product_model_id BIGINT REFERENCES product_models (id) ON DELETE SET NULL,
    ADD COLUMN variant_color VARCHAR(64),
    ADD COLUMN variant_size VARCHAR(32);

CREATE INDEX idx_products_user_supplier ON products (user_id, supplier_id);
CREATE INDEX idx_products_user_model ON products (user_id, product_model_id);

ALTER TABLE nfe_entries
    ADD COLUMN supplier_tax_id VARCHAR(14);
