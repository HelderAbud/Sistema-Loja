# Validação — H5 histórico PDV multi-item

- Data: 2026-09-17
- Branch: `feat/sales-history-multi-item-display`
- Trilha: Normal

## Contexto

A API (`SaleListItemResponse`) já soma quantidade e `lineTotal` e indica “+ N outro(s)”. O ecrã ainda mostrava **P. venda** e **P. custo** da 1.ª linha.

## O que mudou

- `saleListUnitAmount`: se `itemCount > 1`, unitário é `null` (UI mostra —).
- `SalesHistoryTab` usa isso nas colunas de preço/custo unitário; **Total** continua `saleLineTotal`.

## Fora de âmbito

- `SaleRequest` / Venda Rápida.
- Explodir cada `sale_item` em linhas próprias.

## Verificação

- `cd frontend && npm test -- --run src/features/sales/domain/saleListDisplay.test.ts`
