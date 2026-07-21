# Validation — Trilha Dia 9 (Smoke + segurança)

**Data:** 2026-07-21  
**Triagem:** Helder Normal (+ HITL)  
**API:** https://lojapp-api.onrender.com  
**Front:** https://sistema-loja-psi.vercel.app  

## Checklist Dia 9

| Item | Resultado |
|------|-----------|
| Health agregado | OK — HTTP 200 `{"status":"UP","groups":["liveness","readiness"]}` |
| Swagger UI | OK (protegido) — `/swagger-ui.html` e `/swagger-ui/index.html` → **401** |
| OpenAPI | OK (protegido) — `/v3/api-docs` → **401** |
| `verify-api-env.ps1` | OK — Helder confirmou login + brands + products em prod (2026-07-21). Script corrigido (`marca(s)` / sem System.Web). Registo público permanece **403**. |
| README Demo | OK — secção Demo + pitch Estado atualizado |

## Probes extra

| Path | HTTP | Nota |
|------|------|------|
| `/actuator/health` | 200 UP | Público |
| `/actuator/health/readiness` | 401 | Em prod exige auth (Render usa health agregado) |
| `/actuator/health/liveness` | 401 | Idem |
| `POST /api/v1/auth/register` | 403 | `O registo público está desativado` — desejável |

`scripts/verify-deploy-health.ps1` falhou no Windows com `curl` SSL revocation (`CRYPT_E_NO_REVOCATION_CHECK`); revalidado com `curl --ssl-no-revoke` / WebClient.

## Residual

- Subpaths readiness/liveness 401: aceitável no Render free; rever se migrar para orquestrador com probes dedicados.

## Aprovado?

- [x] Fatia Dia 9 verificável (health + Swagger off + verify-api-env + Demo no README)
- [ ] Commit/PR docs — em curso (`PR LojApp`)
