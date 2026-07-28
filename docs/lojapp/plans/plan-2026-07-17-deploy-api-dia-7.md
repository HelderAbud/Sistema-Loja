# Plano Dia 7 — Postgres + API (deploy)

**Data:** 2026-07-17  
**Trilha Helder:** Normal (+ HITL cloud)  
**Pré-requisito:** Dia 6 merged (#44); secrets no `.env` local

## Objetivo

API LojApp no ar (Railway **ou** Render) com Postgres managed; `GET /actuator/health` → UP. Flyway aplica migrations no arranque.

## Fora de escopo

- Frontend / Vercel (Dia 8)
- URLs no README (Dia 9–10)
- Redis gerido (MVP: `LOJAPP_RATE_LIMIT_MODE=memory`)

## Aceite

| Critério | Como verificar |
|----------|----------------|
| Health 200 | `curl https://<api>/actuator/health` → status UP |
| Flyway | logs sem erro de migration; app sobe |
| Secrets | só no painel do provider; zero no Git |
| Swagger | **desligado** em `prod` (esperado) — não exigir UI Swagger |

## Variáveis da API (painel)

| Variável | Valor |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `LOJAPP_JWT_SECRET` | copiar do `.env` local (não colar no chat) |
| `SPRING_DATASOURCE_URL` | JDBC do Postgres managed |
| `SPRING_DATASOURCE_USERNAME` | user do provider |
| `SPRING_DATASOURCE_PASSWORD` | senha do Postgres managed |
| `LOJAPP_CORS_ORIGINS` | `http://localhost:5173` (placeholder até Dia 8) |
| `LOJAPP_RATE_LIMIT_MODE` | `memory` (sem Redis no free tier) |
| `SERVER_PORT` | `8081` (alinhar com Dockerfile; ou `${PORT}` do provider se exigir) |
| `LOJAPP_TRUST_FORWARD_HEADERS` | `true` atrás do proxy HTTPS do provider |

## Fatias

1. **HITL:** escolher provider (Railway | Render)
2. **HITL:** criar projeto + Postgres + ligar repo `HelderAbud/Sistema-Loja` + Dockerfile raiz
3. **HITL:** preencher env + deploy
4. **Verify:** health público; registar URL no grill-log (sem secrets)

## Riscos

- Build Maven no Docker pode estourar tempo/RAM no free tier → aumentar timeout ou build local + image registry
- Porta: provider injeta `PORT` ≠ 8081 → definir `SERVER_PORT` = porta esperada pelo proxy
- Sem `memory` no rate limit, `prod` tenta Redis e pode falhar o arranque
