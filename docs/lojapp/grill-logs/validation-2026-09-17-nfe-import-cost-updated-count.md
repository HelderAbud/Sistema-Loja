# Validação — NFe `productsWithCostUpdated`

- Data: 2026-09-17
- Branch: `feat/nfe-import-cost-updated-count`
- Trilha: Normal

## Contrato

`NfeImportResponse.productsWithCostUpdated` = quantidade de produtos **já existentes** cujo `cost_price` foi actualizado nesta importação (IDs únicos). Fallback (produto novo) não entra neste campo.

## Verificação

- Unitários: `ImportNfeUseCaseTest#execute_existingProductCostUpdated_isCountedOnce`, `NfeProductResolverTest` (`costUpdatedOnMatch`).
- Integração em memória: `LojappCoreServiceTest#importNfe_matchesExistingProductByEan_whenDescriptionDiffers` espera `productsWithCostUpdated == 1`.
- Frontend: `PilotoNfeTab.test.tsx` — aviso «Custo actualizado».
- Maven: `./mvnw -Pci-unit-tests test` no Windows; se PKIX, mesmo perfil no Ubuntu/WSL.

## Fora de âmbito

- Média ponderada de custo.
- `SaleRequest` / Venda Rápida.
