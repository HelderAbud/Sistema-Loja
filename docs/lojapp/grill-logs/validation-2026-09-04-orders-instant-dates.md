# Validação — H6 datas Instant em `/orders`

**Data:** 2026-09-04  
**Branch:** `fix/orders-instant-date-filters`

## Comportamento

Inputs continuam `YYYY-MM-DD`. `listSales` / `summarizeSales` / `summarizeSalesDaily` recebem Instant ISO (`toSalesApiInstantRange`). `SalesHistoryTab` usa o mesmo helper do dashboard.

## Evidência

```text
cd frontend && npm test
```

21 ficheiros, **50 testes**, exit 0 (inclui `dateIsoRange.test.ts` e `salesDateParams.test.ts`: query `from`/`to` com `T…Z`, não `2026-09-01`).

```text
npx tsc --noEmit
```

exit 0.

Railway sem token: `from=2026-09-01` e `from=…Z` devolvem **401** (auth antes do bind).

API local `localhost:8081` continua em baixo (sem Postgres/compose nesta sessão). Sem browser MCP e sem conta demo no Git — smoke visual login → Pedidos **não** foi feito.

Bind autenticado (MockMvc, filtros off):

- `from=2026-09-01` → `MethodArgumentTypeMismatchException` → **500** (não 400; handler não mapeia TypeMismatch).
- `from=2026-09-01T00:00:00.000Z` → **200**.

```text
./mvnw -Pci-unit-tests -Dtest=SaleControllerTest test
```
