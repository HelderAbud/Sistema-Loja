-- Segunda passada do backfill de V29: o motivo livre já está em `reason`.
-- Normaliza `source` categórico nos ajustes manuais antigos (não toca SALE_CANCEL).
UPDATE inventory_movements
SET source = 'MANUAL_ADJUST'
WHERE movement_type = 'ADJUSTMENT'
  AND source IS NOT NULL
  AND source <> 'SALE_CANCEL'
  AND source <> 'MANUAL_ADJUST';
