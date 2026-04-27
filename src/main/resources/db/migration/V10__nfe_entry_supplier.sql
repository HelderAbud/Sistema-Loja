-- Vínculo opcional da entrada de NFe ao fornecedor deduplicado por CNPJ/CPF (passo 3 fundação).

ALTER TABLE nfe_entries
    ADD COLUMN supplier_id BIGINT REFERENCES suppliers (id) ON DELETE SET NULL;

CREATE INDEX idx_nfe_entries_user_supplier ON nfe_entries (user_id, supplier_id);
