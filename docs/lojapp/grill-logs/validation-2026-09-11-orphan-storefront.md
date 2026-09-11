# Validação — limpeza da vitrine órfã

- Data: 2026-09-11
- Branch: `chore/remove-orphan-storefront`
- Trilha: Normal

## Fatia

Remover páginas e módulos da vitrine pública que já não entram no router (só landing, pitch, login e redirects).

## O que mudou

- Apagadas: `HomePage`, `CatalogPage`, `ProductPage`, `CartPage`, `OrdersPage` (+ teste), `SellerAreaPage`.
- Apagados: `features/storefront` (catálogo demo + carrinho) e `features/orders` (só a vitrine usava).
- `storefrontShared.tsx` fica só com `StoreHeader` (Home / Pitch / Entrar).
- Removido `scripts/regenerate_storefront_split.py` (regerava páginas mortas).
- Redirects `/home`, `/catalog`, `/orders`, `/cart`, `/seller`, `/product/:slug` → `/` mantidos.

## Fora de âmbito

- Piloto, PDV, `SaleRequest`, NFe (H13 é fatia seguinte).

## Verificação

- `frontend`: `npm test` (rotas públicas + nav) e `npm run lint` se o ambiente permitir.
- Confirmar que nenhum import aponta para `features/storefront` ou `features/orders`.
