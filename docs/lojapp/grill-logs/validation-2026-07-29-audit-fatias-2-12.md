# Validação — Fatias 2–12 (auditoria LojApp)

Data: 2026-07-29  
Branch: `fix/authz-and-audit-slices`

## Resumo por fatia

| # | Fatia | Evidência |
|---|--------|-----------|
| 2 | Docs Dia 17/18/E + HA local | Ficheiros em `docs/portfolio/`, `deploy/ha/`, `docker-compose.ha.yml`, grill-logs 17/E |
| 3 | README honesto CI/E2E | Secção “O que o CI prova” em `README.md` |
| 4 | Security headers | `SecurityConfig` + `SecurityHeadersSmokeTest` |
| 5 | Password default | `application.yml` sem fallback global; profile `local` |
| 6 | CSRF/cookie Origin | `AuthCsrfGuardFilter` + `docs/lojapp/security-csrf-cookie-threat-model.md` |
| 7 | React.lazy rotas | `frontend/src/App.tsx` |
| 8 | Split StorefrontPages | `frontend/src/pages/storefront/*` + barrel; script `scripts/regenerate_storefront_split.py` |
| 9 | A11y mínimo | `PilotoWorkspacePage` tabs/panels; `PilotoSaleTab` combobox |
| 10 | CI duplicado + Dependabot | `backend-ci.yml` desativado; `dependabot.yml` limit 1; `CONTRIBUTING.md` |
| 11 | Contracts Sales/Auth | `application/contract/*`; aliases deprecated em `service/contract/*` |
| 12 | Testes cash/POS | `OpenCashSessionUseCaseTest`, `CreatePosSaleUseCaseTest`; `LojappCoreServiceTest` excluído em `-Pci-unit-tests` |

## Comandos executados (2026-07-29)

- Backend: `./mvnw "-Pci-unit-tests" test` — **208 testes, 0 falhas**
- Frontend: `npm run test` — **37 testes, 0 falhas**
- Frontend: `npm run build` — **OK** (tsc + vite após correção imports storefront)

## Riscos residuais

- Testes `*IntegrationTest` com Testcontainers não revalidados localmente (Docker indisponível).
- E2E CI continua parcialmente mockado — README documenta o alcance.
- Dev local exige profile `local` ou env para password DB.
- `@PreAuthorize` dual-vocab permanece verboso até unificação de papéis no domínio.
