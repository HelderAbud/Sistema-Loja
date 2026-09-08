# Validação — nav público + pitch honesto

**Data:** 2026-09-08  
**Branch:** `fix/public-nav-and-pitch`

## Comportamento

- Menu público: Home (`/`), Pitch (`/pitch`), Entrar (`/login`).
- Chip sem números inventados: «MVP em demonstração pública · 1 conta = 1 loja».
- Rotas da vitrine (`/home`, `/catalog`, `/product/:slug`, `/cart`, `/orders`, `/seller`) redirecionam para `/`.
- Páginas da vitrine mantidas no código, fora do router.
- Pitch alinhado a `docs/lojapp/pitch-portfolio.md` (NFe XML, stock, PDV, comissão, KPIs, JWT, isolamento). Sem backend nem `/piloto/*`.

## Evidência

```text
cd frontend && npx prettier --write src/App.tsx src/routeDocumentMeta.tsx src/pages/storefront/*.tsx
cd frontend && npm test -- src/pages/storefront/PublicSiteNav.test.tsx
cd frontend && npx tsc --noEmit
```

- Prettier: ficheiros já formatados.
- Vitest focado: 3 testes, exit 0 (`PublicSiteNav.test.tsx`).
- Vitest completo: 23 ficheiros, 57 testes, exit 0.
- `tsc --noEmit`: exit 0.


## Riscos residuais

- Links antigos da vitrine deixam de mostrar catálogo/carrinho (intencional).
- `HomePage` / `CatalogPage` / etc. continuam no bundle só se importadas; hoje só via `StorefrontPages` se algum import residual.
- Verificação visual no browser da Vercel só após deploy.
