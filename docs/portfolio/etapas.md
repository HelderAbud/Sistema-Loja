# LojApp — 7 etapas (portfólio)

Narrativa curta do que o repositório já demonstra. Deploy público: API **Render** + front **Vercel** (Fase B, Dias 6–10).

**Repo:** [HelderAbud/Sistema-Loja](https://github.com/HelderAbud/Sistema-Loja)  
**Evidências visuais:** [`docs/screenshots/`](../screenshots/)  
**Pitch:** [`docs/lojapp/pitch-portfolio.md`](../lojapp/pitch-portfolio.md)

---

## 1. API REST + Flyway + PostgreSQL

Backend em **Java 21 / Spring Boot** com API versionada em `/api/v1`, contratos em OpenAPI e schema evolutivo só via **Flyway** contra **PostgreSQL 16**. Controllers finos, use cases e serviços de domínio; erros no formato `ApiErrorResponse`.

**Tags:** `Java 21` · `Spring Boot` · `Flyway` · `PostgreSQL` · `OpenAPI`

**Evidência:** `GET /actuator/health` local; Swagger em `http://localhost:8000/swagger-ui.html`; migrations em `src/main/resources/db/migration/`.

---

## 2. SPA React + JWT + roles

Frontend **React 19 + Vite**: access token em memória, refresh opaco em cookie HttpOnly, renovação ao abrir a app. Autorização por roles (`USER`, `ADMIN`, `CASHIER`, `SELLER`, `MANAGER`, `REPRESENTATIVE`) com rate limit na API.

**Tags:** `React 19` · `Vite` · `JWT` · `TanStack Query` · `roles`

**Evidência:** [`01-login.png`](../screenshots/01-login.png)

---

## 3. NFe → estoque (transação ACID)

Importação de **XML de NFe** com parse seguro, match de produto, gravação de entrada e atualização de saldo numa transação. Vendas baixam stock; concorrência e isolamento multi-loja cobertos por testes de integração.

**Tags:** `NFe` · `XML` · `ACID` · `idempotência` · `estoque`

**Evidência:** [`05-importacao-xml.png`](../screenshots/05-importacao-xml.png) · [`04-estoque.png`](../screenshots/04-estoque.png) · sequência Mermaid no README (fluxo NFe → stock)

---

## 4. Docker Compose + health checks

Ambiente local com **Compose**: Postgres, Redis e API. Readiness via **Spring Actuator** (`/actuator/health`) antes de considerar o serviço no ar.

**Tags:** `Docker` · `Compose` · `Actuator` · `health`

**Evidência:** `docker compose up -d` + health `UP` (verificação da trilha Dia 1).

---

## 5. CI GitHub Actions

Pipelines em `.github/workflows/` com testes unitários, integração com **Postgres real (Testcontainers)**, guardrails **ArchUnit** e E2E **Playwright** no frontend.

**Tags:** `GitHub Actions` · `Testcontainers` · `ArchUnit` · `Playwright`

**Evidência:** [`ci.yml`](../../.github/workflows/ci.yml) · [`backend-ci.yml`](../../.github/workflows/backend-ci.yml) *(badge formal no README = Dia 4)*

---

## 6. Deploy Render / Vercel ✅

API e Postgres managed no **Render**; SPA no **Vercel** com `VITE_API_BASE` e CORS (`LOJAPP_CORS_ORIGINS`). Smoke: health UP, login → dashboard, `verify-api-env.ps1`.

| Serviço | URL |
|---------|-----|
| Frontend | https://sistema-loja-psi.vercel.app |
| API | https://lojapp-api.onrender.com |
| Health | https://lojapp-api.onrender.com/actuator/health |

**Tags:** `Render` · `Vercel` · `CORS` · `secrets`

**Evidência:** [README Demo](../../README.md#demo) · grill-logs Dias [7](../lojapp/grill-logs/validation-2026-07-17-trilha-dia-7.md)–[9](../lojapp/grill-logs/validation-2026-07-21-trilha-dia-9.md) · [validation deploy](../lojapp/grill-logs/validation-2026-07-22-deploy.md)

---

## 7. Dashboard KPI + curva ABC

Dashboard com faturamento/lucro por marca, top produtos, **curva ABC (80/15/5 %)** e alertas de stock baixo — apoio a compra e precificação no dia a dia da loja.

**Tags:** `Recharts` · `KPI` · `curva ABC` · `dashboard`

**Evidência:** [`02-dashboard.png`](../screenshots/02-dashboard.png) · [`06-relatorios.png`](../screenshots/06-relatorios.png) · GIF [`07-fluxo-principal.gif`](../screenshots/07-fluxo-principal.gif)

---

## Como ler isto numa entrevista

1. Problema (planilha frágil) → solução (NFe + stock + venda + KPI).  
2. Apontar etapas **1–5 e 7** com screenshots.  
3. Mostrar a **etapa 6** com a demo pública (Front Vercel + API Render + health).  
4. Aprofundar com os 3 casos de [`pitch-portfolio.md`](../lojapp/pitch-portfolio.md).
