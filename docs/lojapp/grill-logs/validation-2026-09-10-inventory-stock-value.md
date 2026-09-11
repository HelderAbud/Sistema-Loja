# Grill — KPI valor em estoque (2026-09-10)

**Trilha:** Normal · **Ramo:** `feat/inventory-stock-value-kpi` · retomado 2026-09-11 após #93

## Fechado

- `GET /dashboard/inventory-kpis` inclui `totalStockValue` (custo × saldo).
- Dashboard piloto: «Valor ao custo».
- CASHIER continua 403 neste endpoint.

## Verificado

- WSL: `InventoryServiceTest`, `DashboardControllerTest`
- Vitest: `InventoryKpiSection.test.tsx`

## Residual

- `LojappCoreServiceTest.inventoryKpis_reflectsStock` tem assert extra (perfil H2, não no ci-unit-tests Windows).
