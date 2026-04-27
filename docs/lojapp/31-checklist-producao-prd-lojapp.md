# Checklist único — PRD → produção (LojApp)

Documento operacional para repetir em cada entrega. Funde o fluxo **PM/PO + PRD → implementação → verificação → merge → QA** com rotina **LojApp** (comandos, docs e demo).

**Origens:** `10-guia-junior-piloto-deploy-proximos-passos.md`, `29-resumo-executivo-status-riscos-proximos-passos.md`, `plano-execucao-sprint-1-a-6.md`, `AGENTS.md`. *(Planos opcionais apenas no workspace local: `.cursor/plans/` — ignorados pelo Git.)*

**Nota de nomenclatura:** este ficheiro é o **checklist de fluxo PRD → entrega**. O **Dia 8 do plano 14d** está detalhado em `31-checklist-seguranca-operacional-dia8.md` (outro doc, mesmo prefixo numérico).

---

## Alinhamento com o plano demo / portfólio (estado no repositório)

Referência canónica: `10-guia-junior-piloto-deploy-proximos-passos.md` e `29-resumo-executivo-status-riscos-proximos-passos.md`. Atualizar esta secção quando o roadmap mudar.

| Situação | Dias / tema | Detalhe |
|----------|-------------|---------|
| **Fechado no plano (com evidência)** | 1–5 | Escopo, baseline local, contratos API, E2E (`real-flow` + suite; preferir `CI=true` com API em `127.0.0.1:8080`). |
| **Fechado com ressalva “sintético”** | 7 | Import em lote **validado com fixtures** (`scripts/fixtures/nfe-lote-sintetico-dia7`, `scripts/import-nfe-folder.ps1`). **Não substitui** XML real de piloto. |
| **Preparação sem prova de negócio** | 6 | Docs/tabela em `02-pilotos-e-xmls.md`; **coleta de XMLs reais** e linhas com prova continuam abertas no plano. |
| **Fechado no plano (docs + scripts)** | 8–9 | `31-checklist-seguranca-operacional-dia8.md`, `32-checklist-hardening-deploy-dia9.md`, `verify-auth-errors.ps1`, `verify-deploy-health.ps1`, `verify-compose-prod-config.ps1`. |
| **Pendência operacional explícita** | 9 | Subir stack real `docker compose -f docker-compose.prod.yml up -d` no ambiente candidato e preencher registo no doc 32 (secção indicada lá). |
| **Em aberto no plano** | 10–14, gate diário, critério final 2 semanas | Publicação URL, smoke remoto, screenshots, README, GitHub, pitch; critérios globais do fim das 2 semanas. |

**Armadilhas já registadas no plano:** `npm install` na **raiz** (usar `frontend/`); validação de JSON no PowerShell (`Invoke-WebRequest`) pode dar **falso negativo** — preferir `curl -i` para contratos HTTP.

---

## Como usar

- Marcar `[x]` só com **evidência** (comando OK, print, link, nota com data).
- Executar **por fases**; não pedir à IA “fazer tudo de uma vez”.
- Manter **rastreio PRD → aceite → teste** (uma linha por requisito crítico).

---

## Comandos rápidos (LojApp)

| Objetivo | Comando |
|----------|---------|
| Postgres (Docker) | `docker compose up -d` (na raiz do repo) |
| Compilar API | `mvn -q -DskipTests package` |
| Subir API | `mvn spring-boot:run` |
| Testes unitários (CI profile) | `mvn -B -Pci-unit-tests test` |
| Testes integração (CI profile) | `mvn -B -Pci-integration-tests test` |
| Lint frontend | `cd frontend && npm run lint` |
| Testes frontend (Vitest) | `cd frontend && npm run test` |
| E2E (Playwright) | `cd frontend && npx playwright install chromium && npm run e2e` |
| E2E reprodutível (API local) | `cd frontend && CI=true npm run e2e` |
| Destravar estado local | `.\scripts\destravar-estado.ps1` (ver flags no script) |

**Swagger:** `http://localhost:8080/swagger-ui.html` (porta padrão em `application.yml`).  
**Saúde:** `GET http://localhost:8080/actuator/health` (readiness/liveness quando ativos em `application.yml`).

---

## 1. PRD — entender e alinhar

- [ ] Ler o PRD **várias vezes** (objetivo, fora de escopo, restrições).
- [ ] Listar **perguntas** ao PM/PO (ambiguidade, prioridade, métricas, edge cases).
- [ ] Para cada requisito crítico: **critério de aceite mensurável** + onde será provado (manual / API / E2E).
- [ ] Confirmar **o que não entra** nesta entrega (evitar creep de escopo).

---

## 2. Brainstorm e mapa (com IA)

- [ ] Brainstorm para **lacunas**, riscos e dependências (backend, frontend, DB, auth, NFe, etc.).
- [ ] Mapa único: **épicos → tarefas → ordem** + bloqueadores.
- [ ] Cruzar com escopo oficial: `docs/lojapp/01-escopo-mvp.md` e decisões: `18-decisoes-e-checklist-entrega.md`.

---

## 3. Plano de execução (com IA)

- [ ] Plano **fatiado** (passos verificáveis); rastreio em issues/PR ou em `docs/lojapp/` — não depender de ficheiros só em `.cursor/plans/` para quem faz clone limpo.
- [ ] Itens com **Flyway**: nova migration em `src/main/resources/db/migration/` (não reescrever migrations antigas).
- [ ] Contratos REST: DTOs + Swagger (`springdoc-openapi`); ver `17-versionamento-api-rest.md` se mudar prefixo/versão.

---

## 4. Implementação passo a passo

- [ ] Um passo do plano por vez; **validar** antes do próximo (compila, smoke mínimo).
- [ ] Controllers finos; regra de negócio fora do controller; `user_id` nas entidades operacionais.
- [ ] Novo comportamento: **teste** que prove o comportamento (unitário e/ou integração).

---

## 5. Subir aplicação — smoke técnico

- [ ] API sobe sem erro; `GET /actuator/health` → `UP` / HTTP 200.
- [ ] Se usar frontend contra API local: variáveis `VITE_*` / proxy alinhados ao ambiente (ver `frontend` e docs E2E).

---

## 6. Testes backend (unitário + integração)

- [ ] `mvn -B -Pci-unit-tests test` — verde.
- [ ] `mvn -B -Pci-integration-tests test` — verde (ou justificar skips esperados).
- [ ] Registar contagem de falhas / ambiente (Docker DB, perfis) se algo for condicional.

---

## 7. Testes frontend

- [ ] `cd frontend && npm run lint` — verde.
- [ ] `cd frontend && npm run test` — verde.

---

## 8. E2E (Playwright + API real quando aplicável)

- [ ] `npx playwright install chromium` (se primeira vez).
- [ ] `npm run e2e` ou, para reprodutibilidade com API local, `CI=true npm run e2e` (API tipicamente `127.0.0.1:8080`; credenciais `E2E_REAL_*` quando o cenário for “real flow”).
- [ ] Fora de CI, `reuseExistingServer` no Playwright pode reutilizar preview com config errada — em dúvida usar `CI=true`.
- [ ] Matriz / cenários: `docs/lojapp/24-matriz-cenarios-e2e.md` (atualizar se novos fluxos críticos).

---

## 9. Testes manuais — checklist do fluxo principal

Mínimo para demo/piloto (ajustar rotas à UI atual):

- [ ] **Health** — `GET /actuator/health` OK.
- [ ] **Login e sessão** — área autenticada acessível.
- [ ] **Produto** — criar/listar; stock coerente.
- [ ] **Venda** — registar venda; stock baixa como esperado.
- [ ] **Dashboard** — KPIs / marcas / ABC carregam sem erro (conforme escopo).
- [ ] **NFe — roteiro sintético (engenharia)** — com API a correr: `.\scripts\import-nfe-folder.ps1` sobre `scripts\fixtures\nfe-lote-sintetico-dia7` (chaves fictícias; confirma parser + stock + API).
- [ ] **NFe — piloto real (negócio)** — só após XMLs autorizados fora do Git: coleta Dia 6, import em lote nas pastas piloto, registo e incidentes em `02-pilotos-e-xmls.md` (secções Dia 6 / Dia 7 / Passo 7.2); smoke por loja em `03-implantacao-pilotos.md`.

---

## 10. Segurança e configuração (antes de “go”)

- [ ] Sem secrets no repositório; JWT/DB só em ambiente (`LOJAPP_JWT_SECRET`, etc.).
- [ ] Revisar CORS, cookies/sessão, erros de auth (401/403) — `12-contratos-autenticacao-e-sessao.md`; melhorias contínuas em `20-backlog-seguranca-residual.md`.
- [ ] **Mínimo operacional (plano Dia 8):** `31-checklist-seguranca-operacional-dia8.md`; script auxiliar `scripts/verify-auth-errors.ps1`.
- [ ] **Deploy / hardening (plano Dia 9):** `32-checklist-hardening-deploy-dia9.md`, `21-go-no-go-deploy-producao.md`, `docker-compose.prod.yml`, `scripts/verify-deploy-health.ps1` e `scripts/verify-compose-prod-config.ps1`.

---

## 11. Commit, PR e revisão de código

- [ ] Commits **pequenos e legíveis**; mensagens claras.
- [ ] Abrir PR; revisar **diff completo** (intenção + edge cases + migrations).
- [ ] CI verde no branch (GitHub Actions).
- [ ] **Definition of Done:** `docs/lojapp/27-definition-of-done-unico.md` + `11-checklist-pr-e-convencoes-repositorio.md`.

---

## 12. QA pesado (bateria)

- [ ] Cenários **priorizados** a partir do PRD: happy path, permissões, dados inválidos, regressões.
- [ ] Smoke **por loja piloto** quando houver XML real: `03-implantacao-pilotos.md`.
- [ ] Registrar bugs: severidade, reprodução, se bloqueia entrega.

---

## 13. Ciclo demo / portfolio / piloto (condensado 14 dias)

Use como **segunda passagem** quando o objetivo for demo estável ou case público. A coluna **Plano 14d** corresponde aos mesmos marcos descritos em `10-guia-junior-piloto-deploy-proximos-passos.md` e `29-resumo-executivo-status-riscos-proximos-passos.md` (atualizar quando fechar itens).

| Bloco | Entregável / verificação | Plano 14d |
|-------|---------------------------|-----------|
| Escopo e roteiro | Escopo congelado; roteiro demo 3–5 min; checklist manual alinhado ao §9 | Dias 1–2 fechados no plano |
| Baseline local | App + DB; fluxo E2E/manual sem bloqueio crítico | Dia 2 fechado |
| Contratos API | Swagger + payloads frontend = backend; erros padronizados | Dia 3 fechado |
| E2E principal | Cenário real (API + UI); verde ou backlog priorizado | Dias 4–5 fechados |
| XML sintético (eng) | Fixtures + `import-nfe-folder`; prova técnica de import/stock | Dia 7 fechado **com ressalva** (não é piloto real) |
| XML real (negócio) | Coleta autorizada; registo em `02-pilotos-e-xmls.md`; lote piloto | **Dia 6 aberto**; Dia 7 **repetir** com ficheiros reais |
| Segurança mínima | Env, sessão, auth em erro | Dia 8 fechado (doc 31) |
| Deploy | Perfil alvo; health/readiness; go-live documentado | Dia 9 fechado em docs/scripts; **stack real** ainda pendente no plano |
| Publicação | URL demo; smoke remoto completo | Dias 10–13 **abertos** no plano |
| Portfolio / pitch | Screenshots; README; repo; ensaio | Dias 11–14 **abertos** no plano |

---

## Gate diário (todo dia de execução)

- [ ] O que foi entregue hoje?
- [ ] Qual evidência objetiva (comando, print, log)?
- [ ] Qual bloqueio ficou para amanhã?
- [ ] A meta do ciclo aproximou-se ou o escopo desviou?

---

## Fontes de verdade (navegação)

| Etapa | Documento |
|-------|-----------|
| Escopo go/no-go | `01-escopo-mvp.md` |
| Pilotos e XML | `02-pilotos-e-xmls.md` |
| Implantação por loja | `03-implantacao-pilotos.md` |
| Operação / SLO / backup | `15-operacao-continuidade-filas-s3-slo.md` |
| Aceite e decisões | `18-decisoes-e-checklist-entrega.md` |
| Índice geral | `00-indice-prioridades-sistema.md` |
| Índice técnico | `28-indice-tecnico-unificado.md` |
| Segurança mínima (Dia 8 plano) | `31-checklist-seguranca-operacional-dia8.md` |
| Hardening deploy (Dia 9 plano) | `32-checklist-hardening-deploy-dia9.md` |
| **Este checklist (PRD → entrega)** | `31-checklist-producao-prd-lojapp.md` |

---

## Critério de sucesso do checklist

- [ ] PRD refletido em **aceites** e **testes** rastreáveis.
- [ ] Pipeline local (API + testes + lint + E2E crítico) **verde** ou exceções documentadas.
- [ ] PR revisado; **sem vazamento de secrets**; CI verde.
- [ ] QA registou estado da entrega (pronto / com ressalvas / bloqueado).
