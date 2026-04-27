# Roadmap 6 Sprints (sequencial, checklist, sem implementacao)

## Diretriz central

- Menos CRUD, mais arquitetura.
- Menos stack bonita, mais consistencia operacional.
- Cada sprint fecha com evidencia objetiva (checklist + DoD).

## Status geral de execucao (ate agora)

- [x] Sprint 1 / Item: remover artefatos locais (`node_modules`, `dist`, `target`).
- [x] Sprint 1 / Item: endurecer `.gitignore`.
- [x] Sprint 2 / Item: revisar fluxo de refresh token (rotacao, revogacao, replay e conflitos body/cookie).
- [x] Sprint 2 / Item: documentar contratos de autenticacao — `docs/lojapp/12-contratos-autenticacao-e-sessao.md`.
- [ ] Sprint 1 / Item: remover do versionamento Git (`git rm --cached`) — **adiado**: Git/GitHub so depois do projeto mais maduro; ver `11-checklist-pr-e-convencoes-repositorio.md` (antes do primeiro push).
- [ ] Sprint 1 / Item: revisar historico recente e mapear risco de artefatos grandes — **quando houver repo Git** (ou auditoria manual de pastas grandes antes do primeiro commit).
- [x] Sprint 1 / Item: padronizar estrutura minima de pastas e convencoes — ver `docs/lojapp/11-checklist-pr-e-convencoes-repositorio.md`.
- [x] Sprint 1 / Item: criar checklist de PR (higiene + arquitetura + testes) — `docs/lojapp/11-checklist-pr-e-convencoes-repositorio.md`.
- [x] Sprint 3 / Item: concorrencia de estoque + idempotencia (mapeamento e contrato) — `docs/lojapp/13-estoque-concorrencia-e-idempotencia.md`.
- [x] Sprint 4 / Item: `CreateSaleUseCase`, `AdjustInventoryUseCase`, `ImportNfeUseCase`, `ApplyNfeImportSuggestionsUseCase`, pacote `com.lojapp.application` (incl. `application.nfe`).
- [ ] Sprint 4 / Item (próximas iterações): pacote `domain` dedicado + entidades mais ricas; **iteração 1:** `SaleRegistrationLine`; **iteração 2:** `ManualStockAdjustment`; **iteração 3:** `StockLedgerDelta`; **iteração 4:** `SalePendingCancellation`; **iteração 5:** `NfeStockReceiptLine` (`ImportNfeUseCase`); continuar a reduzir `*Service` genéricos onde fizer sentido.
- [x] Critérios transversais operacionalizados — `docs/lojapp/18-decisoes-e-checklist-entrega.md` (checklist de aceite + modelo de decisões; secção “Critérios transversais” abaixo sincronizada).
- [x] Sprint 6 / Item: exportar e importar presets de filtros (JSON) na tela de pedidos.
- [x] Sprint 6 / Item: tracing HTTP com Micrometer + Brave (`LOJAPP_TRACING_ENABLED`, MDC `traceId`/`spanId`, W3C `traceparent`; export Zipkin opcional `LOJAPP_ZIPKIN_EXPORT_ENABLED` + `LOJAPP_ZIPKIN_ENDPOINT`).
- [x] Sprint 6 / Item: continuidade operacional (filas/retry/DLQ, S3, backup/restore, SLO/alertas) — `docs/lojapp/15-operacao-continuidade-filas-s3-slo.md`, `deploy/prometheus/alerts.lojapp.example.yml`, `scripts/backup-postgres-docker.ps1`, `scripts/restore-postgres-docker.ps1`.
- [x] Sprint 5 / Item: features `storefront`, `auth`, `orders`, `dashboard`, `sales`, `nfe`, `inventory` (UI piloto); guia `14-arquitetura-frontend-por-feature.md`; `shared/async/remoteState.ts`; template `_template`. Revisão residual: `BrandsTab`, `ProductsBrowseTab`, `SalesHistoryTab` em `components/` podem migrar quando conveniente.

## Sprint 1 - Higiene critica de repositorio e baseline tecnico

### Objetivo

Remover passivos que descredibilizam projeto imediatamente.

### Backlog

- [ ] Remover do versionamento `node_modules`, `dist`, `target` (apos `git init` / clone).
- [x] Endurecer `.gitignore` (backend, frontend, IDE, logs, builds).
- [ ] Revisar historico recente e mapear risco de artefatos grandes (com Git; ou checklist pre-commit no doc 11).
- [x] Padronizar estrutura minima de pastas e convencoes — `11-checklist-pr-e-convencoes-repositorio.md`.
- [x] Criar checklist de PR (higiene + arquitetura + testes) — mesmo doc.

### Riscos

- Arquivos "necessarios" estarem indevidamente dentro de `dist/target`.
- Quebra de fluxo local por dependencia implicita de artefatos gerados.

### DoD (Definition of Done)

- [ ] Repositorio sem artefatos versionados (pendente ate primeiro repo remoto/local versionado).
- [x] `.gitignore` cobrindo cenarios reais do projeto.
- [x] Processo de contribuicao com checklist explicito — `11-checklist-pr-e-convencoes-repositorio.md`.

### Observacao (Git / GitHub)

- Primeiro push **adiado** de proposito ate o projeto estar mais elaborado. Ate la: seguir o doc `11-checklist-pr-e-convencoes-repositorio.md` em cada "entrega" local. Quando inicializar Git: `git rm --cached` de artefatos se algum tiver entrado por engano, e revisao de historico se ja houve commits com blobs grandes.

## Sprint 2 - Seguranca de autenticacao (refresh token) e sessao

### Objetivo

Tornar autenticacao previsivel, segura e auditavel.

### Backlog

- [x] Revisar fluxo completo de access + refresh token.
- [x] Definir politica de rotacao de refresh token.
- [x] Definir politica de revogacao (logout, troca de senha, comprometimento).
- [x] Proteger contra replay/reuso indevido de refresh token.
- [x] Especificar erros e respostas padronizadas no ciclo de renovacao.
- [x] Documentar contratos de autenticacao (entrada/saida/expiracao) — `docs/lojapp/12-contratos-autenticacao-e-sessao.md`.

### Riscos

- Alteracao quebrar clientes atuais.
- Lacunas de seguranca por estados intermediarios nao tratados.

### DoD

- [x] Fluxo de refresh com regras explicitas (rotacao/revogacao/reuso).
- [x] Comportamento padronizado para falhas de autenticacao.
- [x] Documento de seguranca de sessao pronto para onboarding tecnico — `12-contratos-autenticacao-e-sessao.md`.

## Sprint 3 - Consistencia transacional (estoque + idempotencia)

### Objetivo

Eliminar risco de venda/estoque inconsistente em concorrencia.

### Backlog

- [x] Mapear pontos criticos de corrida (venda, cancelamento, ajuste) — doc 13; cancelamento ainda inexistente no codigo.
- [x] Definir estrategia de concorrencia (otimista/pessimista por caso) — doc 13 (pessimista em `inventory_balances`).
- [x] Definir invariantes de dominio de estoque — doc 13.
- [x] Introduzir politica de idempotencia para operacoes criticas — doc 13 (estado actual + politica alvo).
- [x] Definir chave idempotente e janela de validade — doc 13 (proposta `Idempotency-Key` + TTL).
- [x] Especificar comportamento em duplicidade/retry/timeouts — doc 13.

### Riscos

- Overhead de lock em rotas de alto volume.
- Idempotencia parcial gerar falsa sensacao de seguranca.

### DoD

- [x] Fluxos criticos protegidos contra corrida no saldo (lock pessimista + transacoes em venda/NFe).
- [x] Contrato de idempotencia implementado para `POST` venda e ajuste stock (`Idempotency-Key`, tabela `api_idempotency`); NFe com chave ja protegida.
- [x] Cenarios de duplicidade/reprocessamento mapeados — doc 13.

## Sprint 4 - Backend orientado a casos de uso (Application + Domain)

### Objetivo

Sair de "service generico" para arquitetura profissional.

### Backlog

- [x] Definir camadas alvo — **em curso**: `application` = `com.lojapp.application.*`; `presentation` = `com.lojapp.controller`; `infrastructure` ≈ `repository` + config; `domain` = `com.lojapp.domain.*` (início: venda) + `entity` JPA como persistência.
- [x] Separar casos de uso principais:
  - [x] `CreateSaleUseCase` (`application/sale`)
  - [x] `CancelSaleUseCase` (`application/sale`; API `POST .../sales/{id}/cancel`)
  - [x] `AdjustInventoryUseCase` (`application/inventory`; núcleo em `InventoryService.applyManualStockAdjustment`)
  - [x] `ImportNfeUseCase` / `ApplyNfeImportSuggestionsUseCase` (`application/nfe`)
- [x] Controllers sem logica de negocio na venda (delegam a `SalesService` / casos de uso).
- [x] Nomear metodos por intencao de dominio nos casos de uso (`execute` + classes por fluxo).
- [x] Reduzir acoplamento: fluxo NFe deixou de concentrar-se em `NfeService` (removido; orquestração em use cases).
- [x] Fronteira transaccional: idempotência + `CreateSaleUseCase` / `AdjustInventoryUseCase`.

### Riscos

- Refatoracao ampla sem estrategia incremental.
- Regressao funcional por falta de cobertura adequada.

### DoD

- [x] Controllers finos (venda/registo com cabeçalho opcional).
- [x] Casos de uso explicitos para fluxos criticos **venda + ajuste stock + import NFe + aplicar sugestões pós-import**.
- [ ] Dominio mais claro que infraestrutura no fluxo principal — **em progresso:** `SaleRegistrationLine`, `ManualStockAdjustment`, `StockLedgerDelta`, `SalePendingCancellation`, `NfeStockReceiptLine`; próximos passos: agregados ricos / menos anémico JPA, ou mais regras NFe (custo, impostos) em `domain.nfe`.

## Sprint 5 - Frontend profissional por feature (nao por paginas)

### Objetivo

Transformar frontend em arquitetura evolutiva.

### Backlog

- [x] Plano de migracao documentado — `docs/lojapp/14-arquitetura-frontend-por-feature.md` + `features/_template/README.md`.
- [x] Estrutura por feature com:
  - [x] `features/` (storefront, auth)
  - [x] `domain/` + `application/` (storefront, auth); `presentation/` opcional / fase seguinte
- [x] Isolar regras de negocio da UI no **storefront** (catalogo demo + totais de carrinho em `domain`).
- [x] Contratos por feature via barrels (`features/storefront/index.ts`, `features/auth/index.ts`).
- [x] Padronizar base de erro/loading — `src/shared/async/remoteState.ts` (extensivel com TanStack Query).
- [x] Reduzir dependencias cruzadas entre telas — **piloto**: `OrdersPage` (feature `orders`); dashboard do piloto migrado para `features/dashboard` (`PilotoDashboardTab` só consome o barrel).
- [x] Template de nova feature — `features/_template/README.md`.

### Riscos

- Migracao longa se tentar "big bang".
- Divergencia de padrao durante transicao.

### DoD

- [x] Primeiras features no novo padrao (**storefront**, **auth**).
- [x] Logica central do storefront (catalogo/totais) fora da UI; auth fora de `hooks` (reexport legacy).
- [x] Guia de arquitetura — `14-arquitetura-frontend-por-feature.md`.

## Sprint 6 - Producao real (observabilidade + escalabilidade operacional)

### Objetivo

Sair de projeto "de portfolio" para operacao seria.

**Próximo passo sugerido após Sprint 5:** implementar itens deste sprint no backend (logs estruturados com correlation id, Actuator/Micrometer, depois filas/SLO conforme backlog abaixo).

### Backlog

- [x] Logs estruturados com correlacao de requisicao (MDC `requestId`/`userId`, header `X-Request-Id`; perfil `jsonlogs` + Logstash encoder em `logback-spring.xml`; compose `docker`/`prod` activam `jsonlogs`).
- [x] Metricas tecnicas e de negocio (histograma + SLO `http.server.requests`; contadores `lojapp.sales.registered`, `lojapp.nfe.imports`, `lojapp.idempotency.replay` — expostos via Prometheus existente).
- [x] Tracing ponta a ponta dos fluxos criticos (camada API: spans HTTP, propagação W3C, correlação em logs/JSON; serviços adicionais e filas = evoluir quando existirem).
- [x] Estrategia de filas/eventos para processos assincronos — doc 15 (decisão; fila física quando houver caso de uso).
- [x] Politica de retry + dead letter — doc 15 (produtor/consumidor + DLQ).
- [x] Estrategia S3 (armazenamento, versionamento, retencao) — doc 15 + `lojapp.nfe.storage` em `application.yml`.
- [x] Estrategia de backup/restore testavel — doc 15 + scripts PowerShell em `scripts/`.
- [x] Definicao inicial de SLO/alertas — doc 15 + `deploy/prometheus/alerts.lojapp.example.yml`.

### Riscos

- Complexidade operacional sem governanca.
- Custo infra subir sem baseline de metricas.

### DoD

- [x] Visibilidade real de saude e falhas (Actuator, Prometheus, logs `jsonlogs`, tracing opcional, regras de alerta exemplo).
- [x] Processos assincronos com resiliencia definida (política em doc 15; implementação de fila quando necessária).
- [x] Plano minimo de continuidade (backup/restore documentado + scripts; alertas exemplo; S3 checklist).

## Criterios transversais (em todos os sprints)

Processo e critérios concretos: **`docs/lojapp/18-decisoes-e-checklist-entrega.md`**. Resumo:

- [x] Cada entrega com checklist de aceite (modelo no doc 18 + `11-checklist-pr-e-convencoes-repositorio.md`).
- [x] Decisoes registradas (modelo ADR curto no doc 18; acrescentar entrada por decisão relevante).
- [ ] Sem expandir escopo alem do sprint — responsabilidade contínua do executor; rever em cada PR.
- [x] Linguagem de dominio explicita em nomes e contratos — em curso via pacote `com.lojapp.domain.*`.
- [ ] Evidencia de qualidade > promessa de qualidade — correr `mvn test` / verificações em `AGENTS.md` antes de declarar concluído.

## KPI de evolucao profissional (para medir progresso)

- Arquitetura: % de fluxos criticos cobertos por use cases explicitos.
- Confiabilidade: taxa de erro em operacoes de venda/estoque.
- Resiliencia: % de operacoes criticas com idempotencia.
- Operacao: cobertura de metricas/traces nos fluxos principais.
- Manutenibilidade: reducao de services genericos e acoplamento cruzado.
- Percepcao tecnica: capacidade de explicar decisoes por dominio, nao por framework.

