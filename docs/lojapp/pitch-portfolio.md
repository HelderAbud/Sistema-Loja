# Pitch de portfólio — LojApp

Documento para **entrevista técnica** e README do GitHub. Tempo alvo: **60–90 segundos** falados + 3 histórias de risco (2–3 min cada, se pedirem aprofundamento).

**Repo:** [HelderAbud/Sistema-Loja](https://github.com/HelderAbud/Sistema-Loja)  
**Evidências visuais:** [`docs/screenshots/`](../screenshots/)  
**Grill log A3:** [`grill-logs/2026-06-02-A3-pitch-portfolio.md`](grill-logs/2026-06-02-A3-pitch-portfolio.md)

---

## 1. Pitch falado (60–90 s)

> **Problema:** Lojas pequenas e revendedores ainda dependem de planilhas — stock errado, nota fiscal à mão e zero visão de margem no dia a dia.  
> **Solução:** O **LojApp** é um monólito **Spring Boot + React** onde a nota **NFe entra por XML**, o **stock atualiza**, a **venda baixa saldo** e o **dashboard** mostra KPIs, marcas e curva ABC — tudo **isolado por conta** (cada loja só vê os seus dados).  
> **Stack:** Java 21, PostgreSQL com **Flyway**, JWT com refresh e rate limit, frontend em React 19 com TanStack Query, **CI no GitHub** com testes unitários, integração com **Testcontainers**, E2E Playwright e scan de imagem.  
> **Diferencial verificável:** Não é só CRUD — há **testes de concorrência de stock**, **isolamento entre utilizadores**, **idempotência em APIs críticas**, **ArchUnit** nas camadas e **migrações validadas** contra Postgres real no pipeline.  
> **Estado:** MVP demonstrável em Docker local; repositório público com screenshots e documentação técnica em `docs/lojapp/`.

**Versão ultra-curta (~45 s):**  
Planilha frágil → LojApp liga NFe, stock, vendas e dashboard numa SPA com API REST versionada. Java 21, Postgres, Flyway, JWT, React 19. Qualidade provada com Testcontainers, testes de concorrência, isolamento multi-loja e CI verde — não só “fiz Spring Boot”.

---

## 2. Três casos para entrevista (risco → mitigação → evidência)

Use o formato **STAR** mentalmente: situação → risco → ação → resultado medível.

### Caso A — Stock negativo com vendas em paralelo

| | |
|---|---|
| **Risco** | Duas vendas simultâneas no mesmo SKU podem deixar stock negativo ou inconsistência entre saldo e movimentos. |
| **O que fizemos** | Venda e ajuste de stock com regras de domínio + teste de integração com **várias threads** e stock inicial limitado (10 unidades). |
| **Evidência** | `SalesConcurrencyIntegrationTest.concurrentSales_doNotLetStockGoNegative` — Postgres via Testcontainers, Flyway `validate`, assert de saldo ≥ 0 e contagem de vendas bem-sucedidas vs. rejeitadas. |
| **Frase para entrevista** | “Tratei concorrência de stock como requisito de negócio, não como detalhe de JPA — e tenho teste de integração que simula corrida com executor paralelo.” |

**Comando para citar no README/entrevista:**

```bash
./mvnw -Pci-integration-tests test -Dtest=SalesConcurrencyIntegrationTest
```

*(Requer Docker no runner ou máquina local.)*

---

### Caso B — Vazamento de dados entre lojas (`user_id`)

| | |
|---|---|
| **Risco** | Em modelo multi-loja (uma conta = uma loja), um utilizador não pode listar ou alterar marcas/produtos de outro `user_id`. |
| **O que fizemos** | Queries e serviços sempre escopados ao utilizador autenticado; tentativas cross-tenant devolvem **not found** (não expõem existência do recurso). |
| **Evidência** | `CatalogIsolationIntegrationTest` — `listAndSearchProducts_returnOnlyOwnerData`, `updateAndDeleteBrand_fromOtherUser_areBlockedByIsolation`, `updateProduct_fromOtherUser_throwsNotFound`. |
| **Frase para entrevista** | “Isolamento não é só filtro no frontend: há testes que provam que o utilizador B não vê nem altera catálogo do A.” |

**Doc de apoio:** [`13-estoque-concorrencia-e-idempotencia.md`](13-estoque-concorrencia-e-idempotencia.md) (regras de domínio).

---

### Caso C — Schema e pipeline — Flyway + Postgres real no CI

| | |
|---|---|
| **Risco** | Migration quebrada ou entidade JPA desalinhada do banco só aparece em produção. |
| **O que fizemos** | **Flyway** versionado em `src/main/resources/db/migration/`; perfil Maven `ci-integration-tests` com `spring.jpa.hibernate.ddl-auto=validate` e testes `*IntegrationTest` contra **PostgreSQL 16** (Testcontainers no CI / serviço Postgres em `backend-ci.yml`). |
| **Evidência adicional** | `LojappLayerArchitectureTest` (ArchUnit) — controllers não acedem a repositories; `ApiVersioningConventionTest` — prefixo `/api/v1` em REST controllers; `RequestCorrelationIntegrationTest` — `X-Request-Id` no `/actuator/health`. |
| **Frase para entrevista** | “O CI não confia só em H2 em memória para o gate de schema: integração sobe Postgres, aplica migrations e valida o modelo Hibernate.” |

**Workflows:** `.github/workflows/ci.yml`, `.github/workflows/backend-ci.yml`.

---

## 3. Diferenciais verificáveis (não marketing)

| Diferencial | Onde ver no repo |
|-------------|------------------|
| Concorrência de stock | `SalesConcurrencyIntegrationTest` |
| Isolamento multi-loja | `CatalogIsolationIntegrationTest` |
| Idempotência HTTP | `V15__api_idempotency.sql`, `SaleControllerTest.registerSale_withAuthentication_forwardsIdempotencyKey`, `InventoryServiceTest.adjustStock_withIdempotencyHeader_*` |
| Guardrails de camadas | `LojappLayerArchitectureTest` |
| Versionamento API | `17-versionamento-api-rest.md`, `ApiVersioningConventionTest` |
| Segurança operacional | JWT + rotação documentada (A2), `.env` fora do Git, CI `repo-hygiene` |
| Observabilidade | `RequestCorrelationFilter`, Actuator, doc `22-observabilidade-rastreabilidade-validacao.md` |
| Portfólio visual | PNG + GIF em `docs/screenshots/`, script `scripts/capture-portfolio-screenshots.ps1` |

---

## 4. Perguntas previsíveis (respostas curtas)

**Por que monólito e não microserviços?**  
MVP e portfólio: um deploy, transações ACID no stock/vendas, menos operação. Contratos internos (`application/*` use cases) preparam extração futura sem over-engineering hoje.

**Como garante qualidade sem “só testes manuais”?**  
Pirâmide: unitários (Mockito + H2 leve), integração (Testcontainers + Flyway validate), ArchUnit, E2E Playwright no CI, Trivy na imagem Docker.

**O que ficou para v2?**  
Deploy público com URL fixa, JaCoCo com threshold no CI, migração completa `components/` → `features/` (ver `25-migracao-frontend-feature-map.md`), hardening B1–B4 do plano consolidado.

**O que NÃO dizer sem nuance:**  
“100% cobertura”, “microserviços”, “IA em produção” — o assistente IA está documentado como fatia (`32`–`38`), não como produto live.

---

## 5. Roteiro de ensaio (5 min)

1. Abrir README no GitHub → screenshots (30 s).  
2. Falar pitch §1 (90 s).  
3. Escolher **um** caso (A ou B) e mostrar ficheiro de teste no IDE (90 s).  
4. Mencionar CI verde + Flyway (30 s).  
5. Fechar com link ao repo e convite para clone + `docker compose up`.

---

## 6. Checklist A3 (auto-validação)

- [x] Pitch 60–90 s escrito  
- [x] 3 casos com evidência (teste ou doc)  
- [x] Diferenciais apontam para ficheiros reais  
- [ ] Ensaiado em voz alta (marcar quando fizeres)  
- [ ] Revisão por par (opcional no DoD do plano)
