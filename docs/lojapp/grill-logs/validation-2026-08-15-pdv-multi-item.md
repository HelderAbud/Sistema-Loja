# Validação — PDV venda multi-item

Data: 2026-08-15  
Branch: `feat/pdv-multi-item`  
Triagem: Helder Normal · skills `tdd` + `database-migration` + `slice-verification`

## Fatia

Checkout PDV com N itens vira **um cupom** (`sales` + `sale_items`) com baixa de stock por linha e ticket médio pelo total do cupom.

## Comandos

- Backend: `./mvnw -Pci-unit-tests test` — **214 testes, 0 falhas** (2026-08-15)
- Frontend: `cd frontend && npm run test -- --run` — **37 testes, 0 falhas**
- Frontend: `npm run lint` — 0 erros (warnings pré-existentes em `storefrontShared.tsx`)
- Integração Ubuntu/WSL (2026-08-15 17:29): `./mvnw -Pci-integration-tests test -Dtest=SalesStockIntegrationTest` — **10 testes, 0 falhas**, BUILD SUCCESS (~3 min). Path `/mnt/c/Users/Pessoal/Desktop/Loja Sistema`. Logs: `Venda cancelada ... lines=1` (dual-write `sale_items`).

## Evidência de comportamento

- `CreatePosSaleUseCaseTest`: 2 linhas, 1 `sales.save`, 2 `saleItems.save`, 2 `decreaseForSale`; duplicado e pagamento errado rejeitados.
- `CancelSaleUseCaseTest`: restaura cada linha; já cancelada não mexe no stock.
- `PosSaleControllerTest`: payload legado e payload `items[]` aceites.
- `CartPage`: checkout com N itens; pagamento = subtotal (sem frete fictício).

## Schema

- `V21__sale_items.sql` + backfill 1:1 a partir de `sales`.
- Dual-write em `CreateSaleUseCase` (piloto).
- KPIs em `SaleRepository` agregam `sale_items`; `averageTicket` = média do total do cupom.

## HITL residual

- Commit/PR (não feito nesta sessão).
- Flyway V21 no próximo deploy Render.
- Smoke manual: abrir turno → 2 produtos no carrinho → finalizar → stock e fecho de caixa.

## Riscos

- Estorno continua total (todas as linhas do cupom).
- Histórico piloto ainda mostra a primeira linha do cupom.
- Postgres free Render: risco de suspensão ~2026-08-16 (fora desta fatia).
