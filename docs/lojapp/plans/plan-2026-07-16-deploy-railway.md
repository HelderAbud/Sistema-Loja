# Plano Dia 6 — preparação deploy (cópia versionada)

> Cópia rastreável do plano local `.cursor/plans/plan-2026-07-16-deploy-railway.md` (pasta `.cursor/` está no `.gitignore`).

**Data:** 2026-07-16  
**Trilha Helder:** Normal  

## Objetivo

Preparar deploy R$ 0 (Railway/Render + Vercel nos dias seguintes): checklist de env e HITL de secrets — **sem** provisionar cloud nem commitar `.env` nesta fatia.

## Checklist env (valores só no hosting / `.env` local)

| Variável | Notas |
|----------|--------|
| `LOJAPP_JWT_SECRET` | ≥32 chars; gerar offline; nunca no chat/Git |
| `POSTGRES_PASSWORD` / `SPRING_DATASOURCE_*` | alinhar com Postgres managed |
| `LOJAPP_CORS_ORIGINS` | placeholder até URL do front (Dia 8) |
| `VITE_API_BASE` | URL da API no build do SPA (Dia 8) |
| `SPRING_PROFILES_ACTIVE=prod` | Swagger off; cookie Secure |

## HITL

- [x] Gerar secrets fora do chat  
- [ ] Confirmar zero secrets no diff do PR  

## Próximo

Dia 7 — Postgres + API no provider escolhido.

**Grill-log:** `docs/lojapp/grill-logs/validation-2026-07-16-trilha-dia-6.md`
