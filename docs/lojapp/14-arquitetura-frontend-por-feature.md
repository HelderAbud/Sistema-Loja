# Arquitetura frontend por feature (LojApp)

Objetivo: **evoluir o React sem CRUD por pasta** — cada capacidade de produto vive em `features/<nome>/` com camadas explícitas, em migração incremental (sem big bang).

## Camadas

| Pasta | Conteúdo | Regras |
|--------|-----------|--------|
| `domain/` | Tipos, invariantes, funções puras (ex.: totais de carrinho, lookups de catálogo demo) | Sem React, sem `fetch`, sem importar `application` ou `presentation`. |
| `application/` | Hooks, Zustand, orquestração de fluxos, integração com `src/api/*` | Pode importar `domain` e infraestrutura partilhada (`api`, `authStore`). |
| `presentation/` | Componentes só desta feature | Importa `application` e `domain`. Opcional enquanto a rota mora em `pages/`. |
| `index.ts` | API pública da feature | Reexport estável para o resto da app. |

Infraestrutura partilhada mantém-se em `src/api/`, `src/components/ui/`, `src/shared/`.

## Estado assíncrono partilhado

`src/shared/async/remoteState.ts` define o discriminado `RemoteState` (`idle` | `loading` | `success` | `error`) para alinhar UI e testes. Com TanStack Query, mapear `isPending` / `isError` / `data` para este modelo quando fizer sentido.

## Features de referência (Sprint 5)

- **`features/storefront`** — `domain/catalog.ts`, `domain/cartTotals.ts`, `application/cartStore.ts`, barrel `index.ts`. A vitrine e rotas públicas continuam em `pages/StorefrontPages.tsx`, que consome só o barrel.
- **`features/auth`** — `domain/types.ts`, `application/useAuthSession.ts`, `application/useLoginForm.ts`. `src/hooks/*` reexporta para não quebrar imports existentes durante a transição.
- **`features/orders`** — histórico de pedidos na vitrine: `domain/` (filtros persistidos, presets JSON, ordenação de linhas, intervalo de datas comparável), `application/useStorefrontOrdersFilters.ts`. A página `OrdersPage` em `StorefrontPages.tsx` orquestra queries e UI; estado de filtros/presets fica no hook + domínio.
- **`features/dashboard`** — piloto executivo: `domain/` (`chartFormat`, agregados `computeBrandKpiSummary`, datas ISO para filtro), `application/useDashboardData` e `useDashboardFilters`, `presentation/*` (gráficos, tabelas, filtros, `PilotoDashboardTab`).
- **`features/sales`** — `domain/saleFormParse.ts` (quantidade, stock insuficiente, debounce), `presentation/PilotoSaleTab`.
- **`features/nfe`** — `domain/nfeApplyEligibility.ts`, `presentation/PilotoNfeTab`.
- **`features/inventory`** — `domain/manualAdjust.ts`, `presentation/PilotoInventoryTab`.

`PilotoWorkspacePage` importa as abas do piloto a partir dos barrels `features/*`.

## Template

Ver `frontend/src/features/_template/README.md`.

## Próximos passos sugeridos

1. Extrair `domain`/`application` de outras áreas (ex.: presets de filtros de pedidos a partir de `StorefrontPages`).
2. Mover gradualmente componentes de `components/` para `features/<nome>/presentation/` quando estiverem claramente ligados a uma capability.
3. Reduzir dependências cruzadas: páginas só importam barrels `features/*` ou hooks públicos, não internals profundos (`features/foo/application/internal`).
