# Validation — Deploy (Dia 10 / DoD Fase B)

**Data:** 2026-07-22  
**Triagem:** Helder Normal  

## DoD Normal

| Critério | Resultado | Evidência |
|----------|-----------|-----------|
| App no ar | OK | Front https://sistema-loja-psi.vercel.app · API https://lojapp-api.onrender.com |
| Health | OK — HTTP 200 `{"status":"UP",...}` (recheck 2026-07-22) | Este log + Dia 7/9 |
| Login funcional | OK | Dia 8 smoke login→dashboard · Dia 9 `verify-api-env.ps1` |

## Stack deploy

| Camada | Provider |
|--------|----------|
| API + Postgres | Render (`lojapp-api`, `lojapp-db`) |
| Frontend | Vercel (`sistema-loja-psi`) |

## Portfólio

- Etapa 6 marcada ✅ em [`docs/portfolio/etapas.md`](../../portfolio/etapas.md)
- README [Demo](../../../README.md#demo)

## Links de validação anteriores

- [Dia 7](validation-2026-07-17-trilha-dia-7.md) — API UP  
- [Dia 8](validation-2026-07-20-trilha-dia-8.md) — front + CORS + login  
- [Dia 9](validation-2026-07-21-trilha-dia-9.md) — Swagger 401 + verify-api-env  

## Residual

- Cold start Render free (~1–3 min no primeiro pedido)
- Cookie refresh `SameSite=Lax` cross-origin: F5 pode pedir login outra vez (MVP)
- Fase C (piloto com dados fictícios) ainda aberta

## Aprovado?

- [x] DoD Fase B verificável
- [ ] Commit/PR — aguarda `PR LojApp`
