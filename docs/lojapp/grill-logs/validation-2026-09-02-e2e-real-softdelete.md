# Grill log — 2026-09-02 e2e real CI + soft-delete produtos

## Fatia

Passos 3 e 4 na branch `feat/e2e-real-ci-product-soft-delete`.

## Verificado

- Unitários Windows: `./mvnw.cmd -Pci-unit-tests test` OK; `LojappCoreServiceTest#deleteProduct_excludesFromSearch` OK.
- Integração Testcontainers (`CatalogIsolationIntegrationTest`): só no CI / Ubuntu.
- Playwright real: só no job `frontend-e2e-real` (Postgres + jar + `npm run e2e:real`).

## Riscos residuais

- Job e2e real aumenta tempo de CI; em paralelo com Trivy, costuma não ser o caminho crítico.
- Soft-delete não tem botão na UI do piloto nesta fatia.
- `e2e:real` local ainda precisa de API em `:8081` e `E2E_REAL_*`; o `webServer` já recebe `VITE_*` via `env` (Windows e CI).
