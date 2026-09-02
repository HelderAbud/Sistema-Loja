-- Soft-delete de produtos: listagens e unicidade de EAN consideram só linhas ativas.
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMPTZ NULL;

DROP INDEX IF EXISTS uq_products_user_ean;
CREATE UNIQUE INDEX uq_products_user_ean ON products (user_id, ean)
    WHERE ean IS NOT NULL AND length(trim(ean)) > 0 AND deleted_at IS NULL;

CREATE INDEX idx_products_user_active ON products (user_id)
    WHERE deleted_at IS NULL;
