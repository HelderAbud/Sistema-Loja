-- Cancelamento lógico de venda: excluída de agregados; stock reposto via movimento de ajuste.
ALTER TABLE sales ADD COLUMN cancelled_at TIMESTAMPTZ NULL;

CREATE INDEX idx_sales_user_cancelled_at ON sales (user_id, cancelled_at);
