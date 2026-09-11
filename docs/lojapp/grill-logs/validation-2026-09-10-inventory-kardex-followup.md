# Grill — Kardex follow-up (2026-09-10)

**Trilha:** Normal · **Ramo:** `feat/inventory-kardex-followup`

## Decisão Flyway

V29 já está em `main`. Não editar checksum. Segunda passada em **V30** (idempotente).

## Fechado

- `source = MANUAL_ADJUST` nos ajustes manuais antigos (depois de `reason` preenchido em V29).
- Combobox de produto no kardex (mesmo padrão visual da Nova venda).
- Quantidade entrada/saída com cor (`qty-in` / `qty-out`).

## Fora

- Não reescrever o combobox da Nova venda nesta fatia (sem risco em venda/caixa).
- KPI valor em estoque continua no stash `wip: inventory stock value kpi`.
