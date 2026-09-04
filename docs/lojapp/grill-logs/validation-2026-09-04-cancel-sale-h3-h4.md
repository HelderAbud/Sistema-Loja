# Validação — Cancelar venda (H3 + H4)

**Data:** 2026-09-04  
**Branch:** `fix/cancel-sale-commission-cash`

## Comportamento

- Caixa `CLOSED` → `SaleCashSessionAlreadyClosedException` (409), sem stock nem delete de comissão.
- Cancel aceite → apaga `CommissionAccrual` da venda na mesma TX.
- Relatório/CSV: query exclui `sale.cancelledAt is not null`.

## Evidência

WSL:

```text
./mvnw -q -Pci-unit-tests -Dtest=CancelSaleUseCaseTest,CommissionReportServiceTest,LojappLayerArchitectureTest test
```

BUILD SUCCESS (exit 0).

## Não corrido

- Integração Testcontainers / H2 `LojappCoreServiceTest`
- Frontend
