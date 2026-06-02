# Assistente / agentes — item 6: observabilidade, limites e primeira corrida

**Status:** guia operacional pós-contrato (2026-04-30).  
**Depende de:** [36](./36-assistente-ia-contrato-papeis-e-roteiro.md), [34](./34-assistente-ia-politica-seguranca.md), [35](./35-assistente-ia-inventario-operacional-e-ator.md).

## 1. Objetivo do item 6

Garantir que cada **corrida** (`runId`) é **auditável**, **segura de repetir** dentro de limites, e que falhas **param o fluxo** em vez de mascarar erros — sem exigir código novo no monólito.

---

## 2. O que registar por corrida (mínimo)

Guardar em ficheiro local **ou** notas internas **fora do git** (nunca commitar tokens).

| Campo | Exemplo | Não incluir |
|-------|---------|-------------|
| `runId` | UUID | — |
| `environment` | `local` | — |
| `startedAt` / `endedAt` | ISO-8601 | — |
| `jwtRole` | `USER` | Email/senha |
| Passos | `planStepId`, método, path, `statusCode` | `Authorization`, refresh, passwords |
| Mutações | Corpo **aprovado** (pode resumir ids) | Dados pessoais desnecessários |
| `HumanApproval` | `approve` + `approvedBy` + notas curtas | — |
| Verificações | Resultado do Verificador (`pass`, evidência) | — |

Se usar LLM: opcionalmente **tokens input/output** e **fornecedor** por `runId` (para custo), sem colar respostas completas com segredos.

---

## 3. Limites e “stop conditions”

| Limite | Comportamento |
|--------|----------------|
| **2× o mesmo `4xx`** no mesmo passo com o mesmo body | Parar; revisar plano ou dados; não retentar em loop. |
| **401** | Parar; renovar sessão (login/refresh) e **novo** `runId` se necessário. |
| **429** | Respeitar `Retry-After`; máximo N tentativas manuais (ex.: 3) documentadas. |
| **Custo LLM** (se aplicável) | Teto por sessão/dia definido à mão; ao ultrapassar, só leitura ou parar. |
| **Duração** | Sessões longas: access JWT expira (~15 min); planear refresh antes de blocos longos (ver [12](./12-contratos-autenticacao-e-sessao.md)). |

---

## 4. Observabilidade da API (já existente)

O LojApp expõe Actuator e métricas de negócio (ex.: auth refresh) — úteis para correlacionar **picos de erro** com corridas do assistente, não para substituir o trace manual do `runId`.

- Saúde: `GET /actuator/health` (conforme `AGENTS.md`).
- Em produção, métricas costumam exigir autenticação; não expor painéis públicos com dados sensíveis.

---

## 5. Checklist — primeira corrida real (após item 5)

- [ ] API e Postgres a correr (`docker compose`, etc.).
- [ ] Utilizador de teste com papel adequado (backoffice **ou** PDV, não misturar sem troca de token).
- [ ] `runId` gerado; roteiro §5 ou §6 de [36](./36-assistente-ia-contrato-papeis-e-roteiro.md) impresso ou à vista.
- [ ] Passos de leitura executados; baseline de stock anotado se for caso de venda/ajuste.
- [ ] `HttpCallProposal` + **`HumanApproval`** antes de **cada** `POST` relevante.
- [ ] `Idempotency-Key` única por mutação (não reutilizar com body diferente).
- [ ] Verificador: `GET` stock ou listagem confirma efeito esperado.
- [ ] Registo da corrida preenchido (secção 2), sem segredos.

---

## 6. Checklist do item 6

- [x] Modelo mínimo de trace por `runId`.
- [x] Stop conditions e limites (HTTP, JWT, LLM).
- [x] Ponte com Actuator/métricas existentes (sem novo código).
- [x] Checklist da primeira corrida real.

---

## 7. Depois do item 6 — sequência sugerida

| Item | Foco | Notas |
|------|------|--------|
| **7** | **Endurecimento do ator B** | **Feito:** `scripts/assistente/` (`env.example`, `http-roteiro-leitura.ps1`, README). Mutações continuam no doc 38 / gate humano. |
| **8** | **Swagger** | **Feito:** `@Operation` / respostas em `InventoryController` e `SaleController`. |
| **9** | **Testes WebMvc** | **Feito:** `SaleControllerTest` alargado (`summary`, `daily`, `registerSale`, `cancel`). |
| **10** | **Módulo assistente no Spring** | **Feito:** pacote `com.lojapp.assistant` + `GET/POST /api/v1/lojapp/assistant/*`; chave `LOJAPP_ASSISTANT_LLM_API_KEY`; quota diária; desativado por defeito (`LOJAPP_ASSISTANT_ENABLED=false`). Ver `application.yml` (`lojapp.assistant`). |
| **11** | **Ampliação fiscal/NFe** | Mesmo padrão dry-run + gate; só com endpoints estáveis e política explícita. |

Ordem recomendada: **7 → 8 → 9** (valor imediato para repetibilidade e contrato); **10–11** quando houver necessidade clara de produto ou fiscal.

---

## 8. Onde parar a documentação da trilha “assistente”

Os itens **1–6** fecham um **MVP de processo**: fatia, API, segurança, inventário, contrato, observabilidade. A partir do **7** o trabalho mistura **implementação opcional** no repo com **hábitos de equipa**; escolhe por prioridade (portfolio, piloto, produção).
