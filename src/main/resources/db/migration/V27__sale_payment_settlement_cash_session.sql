-- Caixa soma pela sessão de liquidação, não só pela sessão da venda.
-- Pendente confirmado depois do fecho entra no turno OPEN atual.
ALTER TABLE sale_payments
    ADD COLUMN settlement_cash_session_id BIGINT REFERENCES cash_sessions (id) ON DELETE SET NULL;

UPDATE sale_payments sp
SET settlement_cash_session_id = (
    SELECT s.cash_session_id FROM sales s WHERE s.id = sp.sale_id
)
WHERE sp.settlement_status = 'CONFIRMED';

CREATE INDEX idx_sale_payments_settlement_session
    ON sale_payments (settlement_cash_session_id);
