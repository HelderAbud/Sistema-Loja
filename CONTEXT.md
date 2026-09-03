# CONTEXT.md — LojApp Pro

Vocabulário acordado no grill inicial (2026-05-24). Usar estes termos em PRs, ADRs e `docs/lojapp/grill-logs/`.

## Domínio

| Termo | Definição | Não confundir com |
|-------|-----------|-------------------|
| **Loja / tenant** | Uma conta de login = uma loja. Dados isolados por `user_id` do próprio utilizador em todas as entidades operacionais | Vários funcionários com papéis diferentes a partilhar o mesmo caixa (fora do MVP) |
| **Papel JWT (`appRole`)** | Perfil do **mesmo** login (o que a API deixa fazer). `CASHIER` e `MANAGER` em contas distintas são duas lojas, não hierarquia | Aprovação de um terceiro na mesma loja |
| **Vendedora (`Seller`)** | Nome no dropdown para comissão; sem senha, sem login, sem papel | Conta de funcionário |
| **Confirmação de diferença (wire: `managerApproval`)** | Auto-declaração do próprio ator: “revisei a diferença acima da tolerância”. Trilha de auditoria, não PIN/role de gerente | Aprovação de gestor de terceiros |
| **Stock / estoque** | Saldo físico de um produto; invariante: nunca negativo após commit | Relatório de inventário ou KPI de dashboard |
| **Ajuste manual** | Mutação de stock via `POST /inventory/adjust`, fora de venda ou NFe | Venda PDV ou baixa automática na venda |
| **Idempotência (HTTP)** | Mesmo `Idempotency-Key` + mesmo fingerprint → mesma resposta, sem efeito duplicado | Cache HTTP ou deduplicação de NFe por hash de XML |
| **NFe (MVP)** | Importação de XML fiscal offline; dedupe sem `chNFe` por hash do conteúdo | Emissão, consulta SEFAZ ou manifestação |

## Arquitetura (backend)

| Termo | Definição | Onde no código |
|-------|-----------|----------------|
| **Use case** | Orquestração transacional: idempotência HTTP, vários agregados, audit | `com.lojapp.application.*` |
| **Service** | CRUD, listagens, relatórios, facades finos | `com.lojapp.service` + `*.contract` |
| **Stock ledger** (alvo PR2) | Mutações de stock com lock pessimista (venda, cancelamento, NFe, ajuste) | Hoje concentrado em `InventoryService` |
| **Seam HTTP → application** | Controller de write injeta contrato de use case, não service com idempotência | Modelo: `CreateSaleUseCase`; exceção atual: `InventoryController` → `adjustStock` direto |

## API e erros

| Termo | Definição |
|-------|-----------|
| **`ApiErrorResponse`** | Corpo JSON padrão: `message`, `code` (`ApiErrorCode`), `status`, `path`, `timestamp` |
| **Mensagem segura** | Cliente não recebe SQL, host interno nem stack; detalhe só em log server-side (`include-message: never` em `application.yml`) |
| **`LojappErrorController`** | Fallback JSON em erros fora do `GlobalExceptionHandler` (filtros, `/error`); deve alinhar à política de mensagem segura (tarefa B1) |

## Ambientes locais

| Ambiente | API | PostgreSQL (dev compose) |
|----------|-----|---------------------------|
| `mvn spring-boot:run` | `localhost:8081` | `loja_db` / `loja_user` (via `.env`) |
| `docker compose up` (dev) | `localhost:8081` | `loja_db` / `loja_user` |
| `docker compose -f docker-compose.prod.yml` | `localhost:8081` | `loja_db` / `loja_user` |

Segredos: apenas em `.env` (gitignored); compose dev exige `${POSTGRES_PASSWORD:?}` e `${LOJAPP_JWT_SECRET:?}`.

## Execução do plano de melhorias (2026-05-24)

- Repositório **público** no GitHub; correções **graduais** conforme `plano-consolidado-melhorias-2026-05-24.md`.
- Ordem acordada: **Fase 0** → **A1 → A2 → A3 → A4** → **B1 → B2 → B3 → B4** → Fases C–F.
- Gate publicação portfolio: Fase A + B + P0 do `CHECKLIST_FINAL.md`.
- Grill obrigatório antes de cada tarefa; log em `docs/lojapp/grill-logs/`.

## Segredos (A2 — acordado 2026-05-24)

- **Rotação JWT:** **Sim** — cada ambiente (local, clone, deploy) gera `LOJAPP_JWT_SECRET` próprio (≥32 bytes); não reutilizar defaults do histórico Git (`lojapp_local_dev_only` no commit inicial).
- Documentar no README na PR de A2: copiar `.env.example` → `.env` e gerar secret antes de `docker compose up`.
