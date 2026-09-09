-- Liquidação HITL: caixa só soma CONFIRMED. Linhas antigas ficam confirmadas.
ALTER TABLE sale_payments
    ADD COLUMN settlement_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE sale_payments
    ADD CONSTRAINT ck_sale_payments_settlement_status
        CHECK (settlement_status IN ('CONFIRMED', 'PENDING'));
