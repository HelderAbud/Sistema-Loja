-- Instantâneo da liquidação HITL. Pendente fica NULL até confirmar.
ALTER TABLE sale_payments
    ADD COLUMN settled_at TIMESTAMPTZ;

UPDATE sale_payments sp
SET settled_at = (
    SELECT COALESCE(s.sold_at, sp.created_at) FROM sales s WHERE s.id = sp.sale_id
)
WHERE sp.settlement_status = 'CONFIRMED';

CREATE INDEX idx_sale_payments_user_settled_at
    ON sale_payments (user_id, settled_at);
