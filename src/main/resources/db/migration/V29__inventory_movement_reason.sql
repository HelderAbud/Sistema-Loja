ALTER TABLE inventory_movements
    ADD COLUMN reason VARCHAR(500);

-- Ajustes manuais antigos gravavam o motivo em `source` (varchar 40).
-- Cancelamento de venda usa source SALE_CANCEL — não copiar.
UPDATE inventory_movements
SET reason = source
WHERE movement_type = 'ADJUSTMENT'
  AND source IS NOT NULL
  AND source <> 'SALE_CANCEL'
  AND source <> 'MANUAL_ADJUST';
