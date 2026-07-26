# Trilha dia a dia — LojApp (Loja Sistema)

> **Metodologia:** [Helder Method v1.2](../Agentes/helder-method-v1.2-resumo-compartilhavel.md) + [skills-pessoal](../Agentes/skills-pessoal/skills-pessoal/README-pt_br.md) ([WORKFLOW](../Agentes/skills-pessoal/skills-pessoal/WORKFLOW.md))  
> **Iniciativa:** Portfólio carro-chefe — apresentação + deploy grátis + piloto  
> **Triagem Helder:** **Normal** (deploy, CORS, secrets; contrato API existente)  
> **Custo:** R$ 0 (Railway/Render/Vercel free tier)  

---

## Como usar

1. **1 dia = 1 fatia vertical** verificável.
2. Dias 📋 / Normal: `to-spec` → `to-issues` antes de editar; Simple: fast path.
3. Salvar planos em `.cursor/plans/plan-YYYY-MM-DD-lojapp-*.md`.
4. Registros de validação: `docs/lojapp/grill-logs/validation-YYYY-MM-DD-*.md` + `slice-verification`.
5. Regra de negócio / bug: `tdd` → Verify com health/CI.
6. **HITL:** commit/push, secrets, deploy, mudança de contrato API.

### Helder → skills-pessoal

| Trilha Helder | Caminho |
|---------------|---------|
| **Simple** | Fast path: fazer → verificar → resumir |
| **Normal** | `to-spec` → `to-issues` → `tdd` (se regra) → `slice-verification` → `code-review` |
| **Complex** | igual Normal + HITL entre fases; `context-discovery` se houver ADR/contrato |
| **Hotfix** | `diagnose` → patch mínimo → regressão → só então retomar a trilha |

### Core Workflow (mapa)

| Fase | Skill |
|------|-------|
| Spec | `to-spec` |
| Plan | `to-issues` |
| Branch | `git-workflow-and-versioning` |
| Build | `tdd` (regra de negócio / bug) |
| Verify | `slice-verification` (health, CI, smoke) |
| Review | `code-review` |
| Simplify | `code-simplification` |
| Ship | `finishing-a-development-branch` |

### Gates HITL

- Rotacionar / definir `LOJAPP_JWT_SECRET` em produção
- Alterar endpoints públicos ou migrations Flyway destrutivas
- Push para repo público com arquivos sensíveis
- Deploy (credenciais Railway/Vercel)
- Commit, push ou PR

---

## Visão das fases (21 dias úteis)

| Fase | Dias | Foco | Trilha |
|------|------|------|--------|
| A — Apresentação | 1–5 | Screenshots, etapas, pitch | Simple |
| B — Deploy | 6–10 | API + front no ar | Normal |
| C — Piloto | 11–15 | 1 loja demo + evidência | Normal |
| D — Polimento | 16–18 | CI badge, README, LinkedIn | Simple |
| E — Opcional local | 19–21 | HA Docker doc (sem AWS) | Simple |

---

## Fase A — Apresentação

### Dia 1 — Auditoria README e screenshots 📋 ✅ (2026-07-08)

**Fatia:** Inventário do que já existe vs placeholder.

**Tarefas**
- [x] Listar screenshots em `docs/screenshots/` — quais são reais?
- [x] Rodar local: `docker compose up` + login + dashboard
- [x] Capturar ou substituir: `01-login.png`, `02-dashboard.png` (mínimo)
- [x] Plano: `.cursor/plans/plan-2026-07-08-screenshots-audit.md`

**Validação**
- [x] App sobe local; health `GET /actuator/health` → UP
- [x] ≥2 screenshots reais no README (01–06 entregues; GIF 07 → Dia 2)

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-08-trilha-dia-1.md`

**Prompt Cursor**
```text
LojApp Dia 1 — trilha Simple. Auditar docs/screenshots, rodar compose,
capturar login e dashboard com dados fictícios. Fast path / to-issues primeiro.
Não alterar código de negócio.
```

---

### Dia 2 — Screenshots restantes + GIF

- [x] Capturar `03-vendas`, `04-estoque`, `05-importacao-xml`, `06-relatorios`
- [x] GIF curto (15–30s): login → dashboard → 1 venda (`07-fluxo-principal.gif`)
- [x] Seguir [`docs/screenshots/README.md`](docs/screenshots/README.md)

**Validação:** README exibe todas as imagens sem link quebrado.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-09-trilha-dia-2.md`

---

### Dia 3 — `docs/portfolio/etapas.md` 📋 ✅ (2026-07-15)

**Fatia:** Narrativa 7 etapas (formato portfólio).

**Conteúdo das 7 etapas**
- [x] API REST + Flyway + PostgreSQL  
- [x] SPA React + JWT + roles  
- [x] NFe → estoque (transação ACID)  
- [x] Docker Compose + health checks  
- [x] CI GitHub Actions  
- [x] Deploy Railway/Vercel *(texto etapa 6 atualizado no Dia 10 → Render/Vercel ✅)*  
- [x] Dashboard KPI + curva ABC  

Cada etapa: parágrafo + tags (`JWT`, `Flyway`, etc.) + screenshot ou “evidência”.

**Validação:** arquivo linkado no README.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-15-trilha-dia-3.md`  
**Plano:** `.cursor/plans/plan-2026-07-15-lojapp-etapas.md`

---

### Dia 4 — Diagrama Mermaid + badge CI ✅ (2026-07-16)

- [x] Adicionar diagrama Browser → SPA → API → Postgres/Redis no README
- [x] Badge CI no topo (workflow `.github/workflows/ci.yml`)
- [x] Verificar CI verde no GitHub

**Trilha:** Simple.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-16-trilha-dia-4.md`

---

### Dia 5 — Pitch ensaio 📋 ✅ (2026-07-16)

- [x] Ler [`docs/lojapp/pitch-portfolio.md`](docs/lojapp/pitch-portfolio.md)
- [ ] Gravar ou ensaiar 60–90s (3 casos técnicos) — **HITL:** voz alta (checklist no pitch §6)
- [x] Resumo 5 linhas no README apontando para pitch completo
- [x] `docs/lojapp/grill-logs/validation-2026-07-16-fase-apresentacao.md`

**DoD Fase A:** screenshots + etapas + diagrama + pitch linkado — **OK** (ensaio oral = gate humano).

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-16-fase-apresentacao.md`

---

## Fase B — Deploy (R$ 0)

### Dia 6 — Preparação deploy 📋 ✅ (2026-07-16)

| Trilha | Normal |

**Tarefas**
- [x] Ler [`docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md`](docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md) (Parte 4)
- [x] Plan: [`docs/lojapp/plans/plan-2026-07-16-deploy-railway.md`](docs/lojapp/plans/plan-2026-07-16-deploy-railway.md)
- [x] Checklist env: `LOJAPP_JWT_SECRET`, `POSTGRES_PASSWORD`, `LOJAPP_CORS_ORIGINS` (documentado)
- [x] **HITL:** gerar secrets offline; não commitar `.env` — OK (Helder, 2026-07-16)

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-16-trilha-dia-6.md`

---

### Dia 7 — Postgres + API (Railway ou Render) ✅ (2026-07-17)

- [x] Criar projeto + Postgres managed (Render `lojapp-db`)
- [x] Deploy API (Dockerfile raiz) → `lojapp-api`
- [x] Flyway aplica migrations (arranque `prod`)
- [x] Testar `/actuator/health` → **UP**  
  - URL: `https://lojapp-api.onrender.com`  
  - Nota: `MANAGEMENT_HEALTH_REDIS_ENABLED=false` + `LOJAPP_RATE_LIMIT_MODE=memory` (sem Redis no free tier)  
  - Swagger desligado em `prod` (esperado)

**Validação:** URL API responde health UP.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-17-trilha-dia-7.md`  
**Plano/runbook:** `docs/lojapp/plans/plan-2026-07-17-deploy-api-dia-7.md` · `docs/lojapp/runbooks/runbook-dia-7-deploy-api.md`

---

### Dia 8 — Frontend (Vercel ou estático) ✅ (2026-07-20)

- [x] Deploy frontend com `VITE_API_BASE` apontando para API  
  - Front: `https://sistema-loja-psi.vercel.app`  
  - API: `https://lojapp-api.onrender.com`
- [x] Ajustar `LOJAPP_CORS_ORIGINS` na API (origem Vercel)
- [x] Smoke: login → dashboard (registo temporário com `LOJAPP_REGISTRATION_ENABLED=true`)

**HITL:** CORS e URLs revistos; conta demo criada em produção.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-20-trilha-dia-8.md`  
**Plano/runbook:** `docs/lojapp/plans/plan-2026-07-20-deploy-frontend-dia-8.md` · `docs/lojapp/runbooks/runbook-dia-8-vercel-cors.md`

---

### Dia 9 — Smoke produção + segurança ✅ (2026-07-21)

- [x] Rodar smoke de produção (`verify-api-env.ps1` OK em Render; health UP)
- [x] Confirmar Swagger desabilitado ou protegido em prod (401 em `/swagger-ui.html` e `/v3/api-docs`)
- [x] Documentar URLs em README (seção Demo)

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-21-trilha-dia-9.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-21-smoke-dia-9.md`

---

### Dia 10 — Atualizar etapas + validation deploy ✅ (2026-07-22)

- [x] Etapa 6 em `docs/portfolio/etapas.md` → ✅ com URLs (Render + Vercel)
- [x] `docs/lojapp/grill-logs/validation-2026-07-22-deploy.md`
- [x] **DoD Normal:** app no ar + health + login funcional

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-22-deploy.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-22-etapas-dia-10.md`

---

## Fase C — Piloto demo

### Dia 11 — Conta loja fictícia 📋 ✅ (2026-07-23)

- [x] Criar usuário demo (dados fictícios) — local Ubuntu  
- [x] Seed ou cadastro manual: produtos (`seed-demo-roupas.sh`) + NFe fixtures sintéticas  
- [x] Plano piloto: `docs/lojapp/plans/plan-2026-07-23-piloto-demo.md`

**HITL:** stack local (API + Vite); seed **não** em Render.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-23-trilha-dia-11.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-23-piloto-demo.md`

---

### Dia 12 — Fluxo ponta a ponta piloto ✅ (2026-07-24)

**Fatia vertical:** NFe entra → estoque atualiza → venda baixa saldo.

- [x] Executar fluxo no deploy (ou local se deploy instável) — **local** WSL; produto 13 qty 2→1
- [x] Screenshots em [`docs/screenshots/piloto/`](docs/screenshots/piloto/) — captura UI real (`piloto-dia12@lojapp.demo`, Chrome + `LOJAPP_API_PROXY`)

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-24-trilha-dia-12.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-24-lojapp-dia-12-piloto-e2e.md`

---

### Dia 13 — Testes de regressão críticos ✅ (2026-07-25)

- [x] `mvn test` / CI verde — local: Surefire 268/0 fail; front lint + Vitest 37 OK
- [x] Se bug encontrado: trilha **Hotfix** (patch mínimo + teste) — N/A (sem falhas)

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-25-trilha-dia-13.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-25-lojapp-dia-13-regressao.md`

---

### Dias 14–15 — Documentar piloto ✅ (2026-07-26)

- [x] 1 página [`docs/lojapp/piloto-demo-resultado.md`](docs/lojapp/piloto-demo-resultado.md) (sem dados reais)
- [x] Validation fase C — `docs/lojapp/grill-logs/validation-2026-07-26-fase-c-piloto.md`

**DoD Fase C:** piloto local E2E + regressão + página de resultado — **OK**.

**Evidência:** `docs/lojapp/grill-logs/validation-2026-07-26-fase-c-piloto.md`  
**Plano:** `docs/lojapp/plans/plan-2026-07-26-lojapp-dia-14-15-piloto.md`

---

## Fase D — Polimento portfólio

### Dia 16 — README final

- [ ] Seção Demo com links clicáveis
- [ ] Como rodar local (5 linhas)
- [ ] Revisão CONTRIBUTING + CHECKLIST_FINAL

---

### Dia 17 — LinkedIn

- [ ] Post com GIF dashboard + link GitHub + link demo
- [ ] 3 bullets técnicos (NFe/transação, JWT, deploy)

---

### Dia 18 — Revisão code-reviewer (simulada)

- [ ] Diff da trilha inteira vs main
- [ ] Rubrica: segurança, secrets, aderência AGENTS.md
- [ ] `validation-YYYY-MM-DD-trilha-completa.md`

---

## Fase E — Opcional (sem AWS)

### Dias 19–21 — HA local documentada

| Trilha | Simple — só documentação |

- [ ] `docs/portfolio/etapa-ha-local.md`: 2 réplicas API + nginx + script curl loop
- [ ] Opcional: implementar compose override se sobrar tempo
- [ ] **Não** claimar AWS ALB — ser honesto

---

## Prompt base (Cursor)

**Uso diário (preferido):** a regra `.cursor/rules/helder-trilha-diaria.mdc` + `AGENTS.md` já amarram Helder + skills. Digite só:

```text
Continuar LojApp
```

**Versionar a fatia (branch → PR, sem merge):**

```text
PR LojApp
```

**Prompt longo (opcional / sessão sem a regra):**

```text
LojApp — Trilha dia N do TRILHA-DIA-A-DIA.md.
Helder [Simple|Normal|Complex|Hotfix] + skills-pessoal.
Normal: to-spec → to-issues → fatia vertical → tdd se regra de negócio
→ slice-verification (health/CI) → code-review proporcional.
HITL em secrets/deploy/API/commit. Referências: AGENTS.md, docs/lojapp/pitch-portfolio.md.
```

---

## Calendário resumido

| Dia | Entrega |
|-----|---------|
| 1–2 | Screenshots |
| 3–5 | etapas.md + diagrama + pitch |
| 6–10 | Deploy |
| 11–15 | Piloto demo |
| 16–18 | README + LinkedIn + validation |
| 19–21 | HA local doc (opcional) |

---

*Trilha v1.1 — 2026-07-09 — Helder v1.2 + skills-pessoal*
