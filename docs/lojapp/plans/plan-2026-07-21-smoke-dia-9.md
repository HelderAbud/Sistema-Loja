# Plano Dia 9 — Smoke produção + segurança

**Data:** 2026-07-21  
**Trilha Helder:** Normal (+ HITL credenciais)  
**Pré-requisito:** Dia 8 — front Vercel + API Render

## Objetivo

Smoke e hardening documentados; URLs Demo no README.

## Aceite

| Critério | Verificação |
|----------|-------------|
| Health prod UP | `GET /actuator/health` |
| Swagger/OpenAPI não públicos | `/swagger-ui.html`, `/v3/api-docs` → 401 |
| Auth JWT + rotas | `verify-api-env.ps1` **ou** smoke manual (checklist 32) |
| README Demo | URLs Front + API + Health |

## Fora de escopo

- Etapa 6 / DoD deploy (Dia 10)
- SameSite cookie cross-origin
- Reabrir registo público

## Ordem

1. Probes health + Swagger  
2. verify-api-env (HITL) ou smoke Dia 8  
3. README Demo + pitch Estado  
4. Grill-log + TRILHA  
