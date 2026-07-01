# ADR 0001 — Idempotencia HTTP somente em application

**Status:** Accepted
**Data:** 2026-06-23

## Contexto

O ajuste manual de estoque tinha dois caminhos com a mesma logica de idempotencia HTTP:

- `InventoryService.adjustStock(..., Optional<String>)`
- `AdjustInventoryUseCase.execute(...)`

Isso deixava o use case sem uso no fluxo HTTP e duplicava a decisao de `Idempotency-Key` dentro da camada de service.

## Decisao

Idempotencia HTTP deve ficar na camada `application`, dentro de use cases que representam uma operacao de API.

Para ajuste manual de estoque:

- `InventoryController` chama `AdjustInventoryUseCaseContract`.
- `AdjustInventoryUseCase` executa `ApiIdempotencyService.runStockAdjust(...)`.
- `InventoryService.applyManualStockAdjustment(...)` mantem apenas a mutacao de estoque, transacao, cache e auditoria.
- `InventoryService.adjustStock(userId, request)` pode permanecer como alias sem idempotencia para seeds/testes e codigo interno legado.

## Consequencias

- Controllers deixam de acionar idempotencia por meio de services largos.
- Services ficam mais proximos da regra de negocio e menos acoplados ao protocolo HTTP.
- Testes de idempotencia devem mirar use cases; testes de mutacao de estoque devem mirar o service core.

## Verificacao

- `InventoryControllerTest` deve provar que o header `Idempotency-Key` chega ao use case.
- `AdjustInventoryUseCaseTest` deve provar que o use case envolve `applyManualStockAdjustment` no shell idempotente.
- Teste de integracao de replay com mesma chave deve chamar `AdjustInventoryUseCase.execute(...)`.
