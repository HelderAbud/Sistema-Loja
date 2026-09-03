# Grill — refresh JWT concorrente + estado da auditoria (2026-09-03)

## #8 Refresh sem dedup

`refreshSession` partilha a mesma Promise que o bootstrap. Dois 401 em paralelo → um `POST /auth/refresh`. Teste: `frontend/src/api.test.ts`.

## Consolidado (auditoria)

| # | Achado | Estado neste repo |
|---|--------|-------------------|
| 1 | managerApproval | Decidido: auto-declaração (uma conta = uma loja). Código/docs na stash `wip-cash-ack-self-declaration` se ainda não mergeados. |
| 2–5 | RBAC/CORS | Já em `main`. |
| 6 | CSV fórmula | Feito nesta working tree (`CommissionReportService`). |
| 7 | vUnCom negativo | Feito nesta working tree (`NfeXmlParser`). |
| 8 | Refresh concorrente | Feito agora (`client.ts`). |
| 9–10 | @Version caixa / fila seller | Nota, fora desta fatia. |
| 11 | Reuso de refresh | Já no backlog `20-backlog-seguranca-residual.md`. |

XSS/storefront: confirmado, sem acção.

## Verificação

`cd frontend && npm run test -- src/api.test.ts`
