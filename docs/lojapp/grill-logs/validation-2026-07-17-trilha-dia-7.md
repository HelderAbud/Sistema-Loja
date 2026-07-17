# Validation — Trilha Dia 7 (Postgres + API no Render)

**Data:** 2026-07-17  
**Triagem:** Helder Normal + HITL cloud  
**Provider:** Render  

## Checklist Dia 7

| Item | Resultado |
|------|-----------|
| Postgres managed | OK — `lojapp-db` (Oregon) |
| Deploy API (Dockerfile) | OK — `lojapp-api` Live |
| Flyway no arranque `prod` | OK — app sobe sem falha de migration após JDBC correto |
| Health público | OK — `https://lojapp-api.onrender.com/actuator/health` → **UP** |
| Swagger em prod | N/A — desligado por `application-prod.yml` (esperado) |

## Configuração operacional (sem secrets)

| Var | Nota |
|-----|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DB` (sem user/pass na URL) |
| `LOJAPP_RATE_LIMIT_MODE` | `memory` |
| `MANAGEMENT_HEALTH_REDIS_ENABLED` | `false` (sem Redis no free tier) |
| `SERVER_PORT` | `10000` |

## Aprendizados

- URL `postgres://user:pass@host/db` **não** serve crua; converter para JDBC sem credenciais embutidas.
- Health `DOWN` com app Live → tipicamente indicador Redis; desligar health Redis no MVP free.

## Próximo

Dia 8 — frontend (Vercel) + `VITE_API_BASE` + `LOJAPP_CORS_ORIGINS` com URL real do SPA.

## Aprovado?

- [x] Fatia Dia 7 verificável (health UP)
- [ ] Commit docs trilha/grill-log — aguarda HITL
- [ ] URLs no README — Dia 9/10
