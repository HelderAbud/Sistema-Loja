CREATE INDEX IF NOT EXISTS idx_sales_user_sold_at
    ON sales (user_id, sold_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_movements_user_product
    ON inventory_movements (user_id, product_id);

CREATE INDEX IF NOT EXISTS idx_nfe_entries_user_created
    ON nfe_entries (user_id, imported_at DESC);
