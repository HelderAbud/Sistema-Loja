# Assistente / agentes — item 5: contrato entre papéis + roteiro executável

**Status:** especificação para orquestração humana ou ferramenta (2026-04-30).  
**Contexto:** fatia C em [32](./32-assistente-ia-fatia-vertical-v1.md); API e DTOs em [33](./33-assistente-ia-mapeamento-api-fatia-c.md) e [35](./35-assistente-ia-inventario-operacional-e-ator.md).

## 1. Papéis e responsabilidades

| Papel | Entrada | Saída | Regra |
|-------|---------|--------|--------|
| **Planeador** | Objetivo em linguagem natural + contexto mínimo (ambiente, papel JWT) | `Plan` com passos ordenados e dependências | Não inventa endpoints; só referencia operações do inventário 33/35. |
| **Executor** | `Plan` aprovado + subset de passos autorizados | `HttpCallProposal` (dry-run) ou `HttpCallExecuted` (após gate) | Não envia mutação sem `HumanApproval` explícito no trace. |
| **Verificador** | Resultados HTTP + opcional `GET` de confirmação | `VerificationReport` (pass/fail + evidência) | Falha bloqueia avanço para o passo seguinte sensível. |
| **Guardião** (opcional) | Pedido de alteração destrutiva ou ambígua | Escalamento humano; sem automação | Alinhado a `AGENTS.md` / política 34. |

Toda a **verdade de negócio** continua na API; estes papéis são **contrato de orquestração**, não novas regras.

---

## 2. Envelope comum

Todas as mensagens devem incluir:

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `schemaVersion` | string | Ex.: `"1.0"` — incrementar só com mudança compatível ou nova versão documentada. |
| `runId` | string (UUID) | Identificador da corrida; reutilizar em todos os passos da mesma execução. |
| `actor` | string | Ex.: `"cursor-human"`, `"script-local"` (ver [35](./35-assistente-ia-inventario-operacional-e-ator.md)). |
| `environment` | string | Ex.: `local`, `staging` — **nunca** assumir produção sem confirmação explícita. |

---

## 3. Tipos de mensagem (contrato lógico)

### 3.1 `UserGoal`

| Campo | Tipo | Obrigatório |
|-------|------|-------------|
| `goal` | string | Sim |
| `constraints` | object opcional | Não |
| `jwtRole` | string | Sim (ex.: `USER`, `CASHIER`) |

`constraints` pode incluir `maxSteps`, `forbidPaths` (lista de prefixos proibidos), `requireExplicitApprovalFor` (`["POST"]` ou lista de paths).

### 3.2 `Plan`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `steps` | array de `PlanStep` | Ordem de execução sugerida. |
| `risks` | array de string | Ex.: “reduz stock”, “efeito fiscal indireto”. |

**`PlanStep`**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | string | Id estável dentro do plano (ex.: `s1`, `s2`). |
| `intent` | string | Uma frase: o que este passo faz. |
| `operationRef` | string | Referência ao inventário: ex.: `GET /api/v1/lojapp/inventory/products/{productId}/stock`. |
| `dependsOn` | string[] | Ids de passos que devem ter `VerificationReport.pass === true` antes. |
| `isMutation` | boolean | Se true, exige gate humano antes de executar. |

### 3.3 `HttpCallProposal` (dry-run)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `planStepId` | string | Liga ao `PlanStep`. |
| `method` | string | `GET`, `POST`, … |
| `path` | string | Path completo com placeholders resolvidos (sem segredos). |
| `headers` | object | Incluir só chaves permitidas; **valor de `Authorization` nunca** no log partilhado — usar placeholder `REDACTED`. |
| `body` | object ou null | Corpo JSON exatamente como irá ser enviado. |
| `idempotencyKey` | string ou null | Obrigatório para mutações suportadas. |

### 3.4 `HumanApproval`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `planStepId` | string | — |
| `decision` | `"approve"` \| `"reject"` \| `"revise"` | — |
| `notes` | string opcional | Motivo ou alteração pedida ao plano. |
| `approvedBy` | string | Identificador humano (inicial, email interno — não PII pública). |
| `approvedAt` | string ISO-8601 | — |

Sem `decision === "approve"` **não** há `HttpCallExecuted` para mutação.

### 3.5 `HttpCallExecuted`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `planStepId` | string | — |
| `statusCode` | number | — |
| `responseSummary` | string ou object | Resumo seguro (ids, totais); truncar listas grandes. |
| `durationMs` | number opcional | — |

### 3.6 `VerificationReport`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `planStepId` | string | Passo verificado (pode ser o mesmo do execute ou um GET de follow-up). |
| `pass` | boolean | — |
| `evidence` | string ou object | Ex.: “GET stock quantity = 18.000”. |
| `checks` | array de `{ "name", "ok", "detail?" }` | Opcional, para relatórios ricos. |

---

## 4. Fluxo mínimo (diagrama textual)

1. Humano define `UserGoal` + `runId`.  
2. **Planeador** emite `Plan`; humano revê e aceita ou pede revisão.  
3. Para cada passo:  
   - Se **leitura**: **Executor** pode emitir `HttpCallProposal` → (opcional aprovação leve) → executar → **Verificador** opcional.  
   - Se **mutação**: `HttpCallProposal` → **`HumanApproval` obrigatório** → executar → **Verificador** com `GET` ou leitura coerente.  
4. Falha 4xx/5xx: registar em `HttpCallExecuted`, não avançar passos dependentes sem novo plano ou correção.

---

## 5. Roteiro executável — fatia C (backoffice, papel `USER` ou equivalente)

Use com **runId** único e token válido. Substituir IDs de exemplo pelos reais após leituras.

| # | Ação | Mutação? | Gate |
|---|------|----------|------|
| 1 | `POST /api/v1/auth/login` (operador) — obter JWT | Não (auth pública) | Operador |
| 2 | `GET /api/v1/lojapp/products?page=0&size=20` — escolher `productId` | Não | — |
| 3 | `GET /api/v1/lojapp/inventory/products/{productId}/stock` — baseline | Não | — |
| 4 | Opcional: `GET /api/v1/lojapp/dashboard/inventory-kpis` | Não | — |
| 5 | Montar `HttpCallProposal` para `POST /api/v1/lojapp/sales` com `SaleRequest` + `Idempotency-Key` | Sim | **Aprovar** corpo e chave |
| 6 | Executar venda; guardar `id` da resposta | Sim | Após gate |
| 7 | `GET .../inventory/products/{productId}/stock` — confirmar decréscimo | Não | **Verificador** |
| 8 | Opcional: `GET /api/v1/lojapp/sales?productId=...` — venda listada | Não | Verificador |

**Ajuste de stock manual (caminho alternativo em vez de ou após venda):**

| # | Ação | Mutação? | Gate |
|---|------|----------|------|
| A | Passos 1–3 como acima | — | — |
| B | `HttpCallProposal` `POST /api/v1/lojapp/inventory/adjust` + `Idempotency-Key` | Sim | **Aprovar** |
| C | Executar; repetir GET stock | — | Verificador |

---

## 6. Roteiro executável — PDV (papel `CASHIER` ou `SELLER` / `MANAGER`)

| # | Ação | Mutação? | Gate |
|---|------|----------|------|
| 1 | Login com utilizador PDV | Não | Operador |
| 2 | `GET /api/v1/lojapp/pos/cash-sessions/current` — se não houver sessão, planear abertura | Não | — |
| 3 | `HttpCallProposal` `POST .../pos/cash-sessions/open` com `openingAmount` | Sim | **Aprovar** |
| 4 | `HttpCallProposal` `POST .../pos/sales/finalize` com `PosSaleFinalizeRequest` + `Idempotency-Key` | Sim | **Aprovar** |
| 5 | Verificar saldo / listagens conforme necessário | Não | Verificador |

---

## 7. Checklist do item 5

- [x] Papéis e limites definidos (Planeador, Executor, Verificador, Guardião opcional).  
- [x] Tipos de mensagem com campos mínimos + `schemaVersion` e `runId`.  
- [x] Roteiros backoffice e PDV com coluna **Gate** explícita.  

**Próximo passo sugerido:** primeira corrida real em ambiente `local` — guia passo a passo em [38-pratica-roteiro-local-ator-a.md](./38-pratica-roteiro-local-ator-a.md) (trace em `local/agent-runs/`, fora do git).
