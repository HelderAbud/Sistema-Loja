# Migração frontend por feature (P2.8)

Estado consolidado para evolução incremental sem big bang.

## Componentes ainda fora de `features/`

### `frontend/src/pages/`

- `StorefrontPages.tsx` (alta concentração de UI + regras de negócio de catálogo/pedidos/resumo)
- `LoginPage.tsx` (auth presentation ainda em `pages/`)

### `frontend/src/components/`

- `ui/*` (infra compartilhada; manter fora de feature por ser cross-cutting)
- `ProductsBrowseTab.tsx` e `SalesHistoryTab.tsx` agora mantidos como reexport de compatibilidade

## Prioridade de migração (impacto)

1. **orders/sales/dashboard primeiro**: telas com maior acoplamento a API, filtros e regras.
2. **storefront em seguida**: extrair blocos de `StorefrontPages.tsx` para `features/storefront` e `features/orders/presentation`.
3. **auth por último nesta fase**: mover `LoginPage` para `features/auth/presentation` sem quebrar rota.

## Redução de acoplamento aplicada nesta iteração

- `PilotoWorkspacePage` passou a importar `ProductsBrowseTab` por `features/products`.
- `SalesHistoryTab` passou a ser exportado por `features/sales`.
- Componentes antigos em `components/` viraram camada de compatibilidade (reexport), evitando quebra durante transição.

## Padrão curto para novas features/telas

- `domain/`: funções puras, tipos e invariantes (sem React/fetch).
- `application/`: hooks e orquestração com query/mutations.
- `presentation/`: componentes da feature.
- `index.ts`: API pública estável (evitar imports profundos).
- Páginas em `pages/` devem consumir apenas barrels `features/*` e `shared/*`.
