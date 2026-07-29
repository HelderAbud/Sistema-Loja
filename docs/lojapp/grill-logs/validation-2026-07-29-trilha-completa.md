# Validation — Trilha completa (Dia 18 · code-reviewer simulado)

**Data:** 2026-07-29  
**Triagem:** Helder Simple · skill `code-review` (documental)  
**Base Git:** `Principal` @ `c6cf7b1` (+ working tree local Dia 17 ainda não commitado)  
**Escopo:** Fases A–D Dias 1–17 (Dias 19–21 HA fora)

---

## 1. Diff / histórico da trilha (vs `Principal`)

Não há um único branch “trilha” — o trabalho entrou por PRs em `Principal`. Marcos relevantes (2026-07):

| Fase | Dias | Evidência / PR (exemplos) |
|------|------|---------------------------|
| A Apresentação | 1–5 | grill-logs 08–16 Jul · PR #23 GIF, #36 etapas, #43 pitch/CI |
| B Deploy | 6–10 | #44–#48 · Render API + Vercel front · validation deploy 22 Jul |
| C Piloto | 11–15 | #52–#54 · E2E local + regressão + página resultado |
| D Polimento | 16–17 | #55 README · Dia 17 rascunho LinkedIn (local, HITL publicar) |
| Extra | ports | #56 matriz 8081/5173/5433/6381 |

Grill-logs presentes: Dias **1–4, 6–9, 11–13, 14–15 (fase C), 16, 17** + fase A + deploy. Dia 5 = ensaio oral (HITL no pitch). Dia 10 coberto em `validation-2026-07-22-deploy.md`.

---

## 2. Rubrica

### 2.1 Secrets e higiene Git

| Critério | Veredito | Evidência |
|----------|----------|-----------|
| `.env` não versionado | **OK** | `gitignore` + `git check-ignore` → `.env`; `git ls-files` sem `.env` |
| Só exemplos no Git | **OK** | `.env.example`, `scripts/assistente/env.example`; `!.env.example` |
| PEM / chaves | **OK** | `*.pem` no `.gitignore`; sem `id_rsa` no índice |
| CI `repo-hygiene` | **OK** | job no `ci.yml` (bloqueia `.env` no índice) |
| Secrets em docs/posts | **OK** | LinkedIn Dia 17 sem passwords; piloto com e-mails fictícios |

**Residual:** histórico antigo do repo pode ter tido JWT de demo (documentado em A2 / pitch) — rotação por ambiente continua obrigatória; não reabrir rewrite sem HITL.

### 2.2 Segurança (produto / deploy)

| Critério | Veredito | Nota |
|----------|----------|------|
| Swagger em prod | **OK** | Dia 9: protegido/desligado (401) |
| CORS explícito | **OK** | Vercel em `LOJAPP_CORS_ORIGINS`; local 5173 (#56) |
| JWT obrigatório | **OK** | `LOJAPP_JWT_SECRET` em prod; compose exige env |
| Rate limit free tier | **Aceitável** | `memory` sem Redis no Render |
| CVEs imagem | **OK** | bump Netty #50 (Trivy) |
| Isolamento `user_id` | **OK** | testes + pitch Caso B |

**Residual:** cold start Render (health timeout ocasional); registo público deve permanecer **off** em prod salvo janela controlada.

### 2.3 Aderência a `AGENTS.md`

| Critério | Veredito |
|----------|----------|
| Controllers finos / Flyway / JWT | **OK** — stack e camadas mantidas |
| HITL secrets/deploy/commit | **OK** — fluxo PR LojApp, sem merge automático pelo agente |
| Portas canônicas | **OK** — alinhadas após #56 |
| Sem inventar Postgres SaaS / n8n / etc. | **OK** — deploy free tier documentado honestamente |
| Fatias com grill-log | **OK** — quase todos os dias com evidence |

**Lacunas conscientes (não blockers da trilha):**

- Ensaio oral pitch §6 ainda aberto (Dia 5 HITL)
- LinkedIn **publicado** ainda aberto (Dia 17 HITL — combinar depois)
- Dia 17 artefacts ainda **uncommitted** no working tree local
- `plano-consolidado-melhorias-*.md` pode citar portas antigas (histórico)

### 2.4 Qualidade / DoD por fase

| Fase | DoD trilha | Status |
|------|------------|--------|
| A | screenshots + etapas + diagrama + pitch | **OK** (ensaio oral = HITL residual) |
| B | app no ar + health + login | **OK** |
| C | piloto E2E + regressão + página | **OK** |
| D (16–17) | README + rascunho LinkedIn | **OK** no repo; publicar LinkedIn = pendente humano |
| D (18) | esta revisão | **OK** (este documento) |

---

## 3. Blockers / Important / Minor

**Blockers (merge / portfólio público):** nenhum novo nesta revisão.

**Important (fazer antes de “fechar carreira” desta iniciativa):**

1. Publicar LinkedIn (Dia 17 HITL) quando fores — com ajuda do agente no texto/mídia se precisares  
2. Commit/PR dos ficheiros Dia 17 (+ este Dia 18) via `PR LojApp`  
3. Confirmar health Render após wake (não tratar timeout frio como down permanente)

**Minor:**

- Limpar leftovers históricos de portas em docs não operacionais  
- Completar ensaio oral do pitch

---

## 4. Veredito do revisor simulado

**PRONTO PARA PORTFÓLIO** com ajustes HITL listados (LinkedIn + versionar Dia 17/18).  
Não declarar “PRONTO PARA PRODUÇÃO enterprise” — é MVP free-tier com cold start e rate limit em memória, e isso está documentado de forma honesta.

---

## 5. Próximos passos

1. **HITL LinkedIn** (quando quiseres — com ajuda)  
2. `PR LojApp` para Dia 17 + 18  
3. Opcional Fase E Dias 19–21 — HA local **só doc**
