# Grill — PR1 (conectar AdjustInventoryUseCase)

**Data:** 2026-06-23
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — Fase C / PR1
**ADR:** `docs/adr/0001-idempotencia-somente-em-application.md`

## Escopo

Conectar o endpoint `POST /api/v1/lojapp/inventory/adjust` ao `AdjustInventoryUseCase`, removendo a duplicacao de idempotencia HTTP do `InventoryService`.

## Perguntas respondidas

| # | Pergunta | Resposta |
|---|----------|----------|
| 1 | O que o use case faz? | Calcula fingerprint do ajuste e executa `ApiIdempotencyService.runStockAdjust(...)` antes de chamar `applyManualStockAdjustment(...)`. |
| 2 | O que o service fazia a mais? | Repetia o mesmo shell idempotente em `adjustStock(..., Optional<String>)`. |
| 3 | Onde fica `runStockAdjust`? | `ApiIdempotencyService`, scope `STOCK_ADJUST`, persistido em `api_idempotency`. |
| 4 | Qual teste protege replay? | `SalesStockIntegrationTest.adjustStock_sameIdempotencyKey_singleMovement`, migrado para o use case. |
| 5 | Precisamos ADR? | Sim — idempotencia HTTP passa a ser decisao da camada application. |

## Decisoes

- [x] Criar `AdjustInventoryUseCaseContract`.
- [x] Controller usa contrato do use case para o ajuste manual.
- [x] `InventoryService.applyManualStockAdjustment(...)` fica como nucleo transacional.
- [x] Manter alias `adjustStock(userId, request)` sem idempotencia para reduzir churn em testes de seed.

## Validacao

Executada nesta sessao:

```powershell
mvn test -Dtest=AdjustInventoryUseCaseTest,InventoryControllerTest,InventoryServiceTest
mvn test -Dtest=SalesStockIntegrationTest#adjustStock_sameIdempotencyKey_singleMovement
mvn test -Dtest=LojappLayerArchitectureTest
mvn test
```

- `AdjustInventoryUseCaseTest,InventoryControllerTest,InventoryServiceTest`: **BUILD SUCCESS** — 17 testes, 0 falhas.
- `SalesStockIntegrationTest#adjustStock_sameIdempotencyKey_singleMovement`: **BUILD SUCCESS**, mas **skipped** por Testcontainers sem Docker valido nesta sessao.
- `LojappLayerArchitectureTest`: **BUILD SUCCESS** — 5 testes, 0 falhas.
- `mvn test`: **BUILD SUCCESS** — 268 testes, 0 falhas, 39 skipped (integrações/Testcontainers sem Docker valido).

## Proximo passo

Depois de verde, atualizar `plano-consolidado-melhorias-2026-05-24.md` com a evidencia real e preparar PR/merge conforme HITL.
