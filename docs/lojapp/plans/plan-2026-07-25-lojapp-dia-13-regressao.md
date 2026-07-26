# Plano — Dia 13 regressão crítica

**Data:** 2026-07-25  
**Trilha Helder:** Normal  
**Fatia:** Provar suite de testes estável após piloto Dia 11–12.

## Aceite

| Critério | Como |
|----------|------|
| Backend | `./mvnw test` exit 0 |
| Frontend smoke | `npm run lint` + `npm run test` em `frontend/` |
| Docs | grill-log + checkboxes TRILHA |

## Inclui

- Fix `frontend/tsconfig.json`: sem `baseUrl` deprecated

## Fora de escopo

- Documentar piloto (Dia 14–15)  
- Upgrade react-router 8.x  
