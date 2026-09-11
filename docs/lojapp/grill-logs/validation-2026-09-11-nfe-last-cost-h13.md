# Validação — H13 último custo no match NFe

- Data: 2026-09-11
- Branch: `chore/remove-orphan-storefront` (fatia 2, após limpeza da vitrine)
- Trilha: Normal

## Política

Match por EAN ou nome actualiza `cost_price` para o `vUnCom` da linha. Não calcula média ponderada. Não altera `sale_price`. Fallback novo continua custo = venda = `vUnCom`.

## Verificação

- Unitários: `NfeLastPurchaseCostTest`, `NfeProductResolverTest`.
- Integração: `importNfe_matchesExistingProductByEan_whenDescriptionDiffers` espera custo `3.00`.
- `./mvnw -Pci-unit-tests test` no Windows; integração com H2/Testcontainers no Ubuntu/CI se o Windows falhar PKIX.

## Fora de âmbito

- `SaleRequest` / Venda Rápida.
- Páginas do piloto.
