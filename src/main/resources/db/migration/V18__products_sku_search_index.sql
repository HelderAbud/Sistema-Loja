-- Pesquisas por SKU por utilizador (autocomplete / catálogo).
CREATE INDEX IF NOT EXISTS idx_products_user_sku
    ON products (user_id, sku)
    WHERE sku IS NOT NULL AND length(trim(sku)) > 0;
