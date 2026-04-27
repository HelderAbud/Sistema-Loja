-- Integridade de saldo, idempotência NFe e valores de movimento válidos (PostgreSQL / H2 em testes).

ALTER TABLE inventory_balances
    ADD CONSTRAINT chk_inventory_balances_quantity_non_negative
        CHECK (quantity >= 0);

ALTER TABLE inventory_movements
    ADD CONSTRAINT chk_inventory_movements_type
        CHECK (movement_type IN ('SALE', 'ENTRY', 'ADJUSTMENT'));

CREATE INDEX idx_nfe_entries_access_key ON nfe_entries (access_key);
