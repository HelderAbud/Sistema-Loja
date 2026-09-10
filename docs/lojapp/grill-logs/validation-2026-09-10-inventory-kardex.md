# Grill — Kardex de estoque (2026-09-10)

**Trilha:** Normal  
**Ramo:** `feat/inventory-kardex`  
**Fora:** venda, caixa, comissão, NFe, valor total em stock (KPI)

## Fechado

- Flyway V29: `inventory_movements.reason` + backfill de ajustes manuais antigos.
- Ajuste manual: `source=MANUAL_ADJUST`, `reason` na linha; `AuditService` inalterado.
- `GET /api/v1/lojapp/inventory/products/{productId}/movements` (paginado, 404 se produto de outra loja).
- Piloto: secção Histórico de movimentos.

## Verificado

- WSL: `./mvnw -Pci-unit-tests -Dtest=InventoryControllerTest,InventoryServiceTest,InventoryMovementRepositoryTest test`
- Front: `npm test -- src/features/inventory/presentation/PilotoInventoryTab.test.tsx`
- Maven Windows: PKIX (BOM Netty) — não corrido neste PC.

## Residual

- KPI “valor em stock” (`costPrice × quantidade`) não entrou nesta fatia.
- Integração Testcontainers / CI completa não corrida aqui.
