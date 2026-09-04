# Cobertura e valor dos testes — LojApp

> Cópia de trabalho no repo: este ficheiro vive em `docs/reviews/`.

| Campo | Valor |
|--------|--------|
| **Data** | 2026-09-04 |
| **Escopo** | Backend (`src/test/java`), frontend Vitest (`frontend/src/**/*.test.*`), Playwright (`frontend/e2e`), CI (`.github/workflows/ci.yml`) |
| **Método** | Inventário estático: classes de produção vs `*Test.java` / `*.test.ts(x)`, contagem de `@Test` / `it(`, amostragem de asserções. **Não há JaCoCo** no POM nem no CI — os percentuais abaixo são cobertura de *classes com teste dedicado*, não linhas executadas. Testes não foram corridos neste passe. |
| **Companheiro** | Achados de bugs/segurança no mesmo dia: [`code-review-repositorio-2026-09-04.md`](code-review-repositorio-2026-09-04.md) |

## Veredito

A suíte é **boa onde o produto dói** — estoque, isolamento por `user_id`, NFe, idempotência, concorrência e papéis. O volume (361 `@Test` Java + ~50 Vitest) não é teatro.

O que falta não é “mais WebMvcTest de controller”. É fechar caixa com teste de regra, o `JwtAuthFilter`, o dashboard no frontend, e um gate JaCoCo para o percentual não cair em silêncio.

| Métrica | Valor |
|---------|--------|
| Classes Java de produção | 236 |
| Classes `*Test.java` | 75 (+ 1 suporte `TestJwtAuth`) |
| Métodos `@Test` (backend) | 361 |
| Controllers com WebMvcTest | 16 / 16 |
| Integrações Testcontainers | 10 classes |
| Frontend `src` TS/TSX (excl. testes) | 93 |
| Ficheiros Vitest | 19 (~50 `it`) |
| Specs Playwright | 4 (2 de produto, 2 de captura de screenshot) |
| Gate JaCoCo no CI | **Não existe** (`docs/CHECKLIST_FINAL.md` Passo 6 / P2 ainda aberto) |

## Mix de valor

Classificação por o que o teste **prova**, não pela pasta.

| Categoria | Estimativa | Critério |
|-----------|------------|----------|
| **Alto valor** | ~65% | Falharia se a regra de negócio, isolamento ou invariante de stock/dinheiro quebrasse |
| **Médio** | ~30% | Contrato HTTP com use case mockado, parsers, utils |
| **Baixo** | ~5% | Smoke de wiring, render de chrome, captura de PNG |

---

## Cobertura por camada (teste dedicado)

Percentagem de classes de produção com um `*Test` próprio. Services e application **parecem mais baixos do que estão**: catálogo, dashboard e NFe entram em `LojappCoreServiceTest` e nos Testcontainers.

| Camada | Produção | Teste dedicado | % |
|--------|----------|----------------|---|
| Controllers | 16 | 16 | 100% |
| Domain | 9 | 8 | 89% |
| Application (excl. `contract/`) | 14 | 7 | 50% |
| Service (excl. `contract/`) | 22 | 8 | 36% |
| Security | 9 | 4 | 44% |
| Frontend `src` | 93 | 19 ficheiros de teste | ~20% |

### Application — o que tem / o que não tem classe própria

| Classe | Teste dedicado | Coberto por outro sítio? |
|--------|----------------|--------------------------|
| `CreateSaleUseCase` | sim | + integração stock/idempotência |
| `CreatePosSaleUseCase` | sim | + concorrência POS |
| `CancelSaleUseCase` | sim | integração restaura stock; **não** cobre comissão (ver code review H3) |
| `ImportNfeUseCase` | sim | + core + Testcontainers |
| `AdjustInventoryUseCase` | sim | + idempotência em `SalesStockIntegrationTest` |
| `OpenCashSessionUseCase` | sim | — |
| `PosSaleCommissionService` | sim | — |
| `CloseCashSessionUseCase` | **não** | só HTTP mockado em `CashSessionControllerTest` |
| `GetCashSessionClosePreviewUseCase` | **não** | idem |
| `GetCurrentCashSessionUseCase` | **não** | — |
| `ApiIdempotencyService` | **não** | exercido de ponta a ponta em `SalesStockIntegrationTest` |
| `ApplyNfeImportSuggestionsUseCase` | **não** | `LojappCoreServiceTest` |

### Services sem `*Test` próprio (amostra)

Cobertos de lado, não “órfãos”: `LojappCatalogService`, `DashboardService` (`LojappCoreServiceTest` + `DashboardLoadIntegrationTest`).

Lacunas reais de teste local: `JwtAuthFilter`, `NfeProductResolver`, `AuthLogoutUseCase` / `AuthRefreshUseCase` / `AuthRegisterUseCase` (auth está em `AuthServiceTest` + integrações de sessão).

---

## O que é forte

### Núcleo H2 — `LojappCoreServiceTest` (38 testes)

`@SpringBootTest` + `@Transactional` + H2. Percorre venda, estoque baixo, KPI de marca (ordenação, paginação, offset), ABC, import NFe (EAN, `cEANTrib`, duplicata de chave, XML sem chave *igual*), CRUD de marca/produto.

Não é mock de controller. Excluído do perfil `ci-unit-tests` (corre no `mvn test` completo, não no Windows unitário).

### Testcontainers (10 classes, Postgres 16)

Correm no CI (`-Pci-integration-tests`) e no Ubuntu/WSL. No Windows local deste PC, Docker não existe.

| Classe | O que prova |
|--------|-------------|
| `SalesStockIntegrationTest` | Baixa de stock, rollback se insuficiente, idempotência venda/ajuste/POS, cancel restaura stock, POS sem caixa / pagamento divergente |
| `SalesConcurrencyIntegrationTest` | Vendas paralelas não deixam stock negativo; POS concorrente sem pagamento duplicado |
| `CatalogIsolationIntegrationTest` | Tenant A não lista/edita/apaga dados de B |
| `NfeImportStockIntegrationTest` | Produto+saldo, CNPJ emitente, chave duplicada, XML namespaced, `cEANTrib` |
| `NfeImportGuardrailsIntegrationTest` | Limite de itens, XML enorme, blank, malformado |
| `DashboardLoadIntegrationTest` | Métricas consistentes em volume; ABC estável em empate |
| `UserAuthorizationIntegrationTest` | Admin list 401/403/200 por papel |
| `AuthSessionIntegrationTest` | Refresh roda cookie e invalida o token anterior |
| `RequestCorrelationIntegrationTest` | Eco / geração de `X-Request-Id` |
| `PerformanceP2SmokeIntegrationTest` | Smoke P95/P99 — não é teste de regra |

### Domain + use cases + ArchUnit

Ledger (`StockLedgerDelta`), linhas de venda/NFe, comissão (`CommissionAmount`, `CommissionRulePicker`, `SellerRoundRobin`), cancelamento pendente.

`LojappLayerArchitectureTest`: controller não depende de repository; service/application não conhecem HTTP.

### Frontend que vale

- Domínio: parse de venda (vírgula), ajuste manual, KPIs de marca, totais do carrinho, date range, papéis (`backofficeAccess`).
- Apresentação com comportamento: `PilotoSaleTab` (stock insuficiente, `sellerId`), `PilotoNfeTab` (import + aplicar sugestão + erro de API).
- `api.test.ts`: erro estruturado e **um único refresh** quando dois 401 disparam em paralelo.
- Playwright **real**: `e2e/real-flow.spec.ts` — login → venda → dashboard contra API viva (job `frontend-e2e-real` no CI).

---

## O que é médio ou pouco valioso

| Teste | Tipo | Julgamento |
|-------|------|------------|
| 16 `WebMvcTest` de controller | Médio | Úteis para contrato JSON, Bean Validation e 403 por papel. Com `@AutoConfigureMockMvc(addFilters = false)` e use case mockado, o happy path só confirma `jsonPath` — **não prova estoque nem caixa** |
| Utils (`EanNormalizer`, `TaxIdNormalizer`, `Pageables`, `ClientIpResolver`) | Médio | Baratos e legítimos; não substituem fluxo |
| `LojappApplicationTest.contextLoads()` | Baixo | Corpo vazio. Já há dezenas de `@SpringBootTest` |
| `MicrometerTracingBridgeContextTest` | Baixo | `assertThat(tracer).isNotNull()` — wiring, não tracing |
| `PageHeader.test.tsx` | Baixo | Render de título/lead |
| `e2e/capture-piloto-dia12.spec.ts`, `capture-portfolio-screenshots.spec.ts` | Baixo | Geradores de PNG para o portfólio, não regressão |
| `e2e/session.spec.ts` | Médio | UI de login/logout com **API mockada** — não prova o backend |

`CashSessionManagerApprovalRequiredExceptionTest` parece “teste de exceção”, mas documenta uma decisão de produto (mensagem sem “gestor”). Manter.

---

## Pirâmide no CI

Definido em `.github/workflows/ci.yml` e perfis Maven em `pom.xml`.

| Job / perfil | O que corre | Onde |
|--------------|-------------|------|
| `backend-unit` / `-Pci-unit-tests` | Unitários; **exclui** `*IntegrationTest` e `LojappCoreServiceTest` | Windows local e CI |
| `backend-integration` / `-Pci-integration-tests` | Só `*IntegrationTest` + Testcontainers | CI e Ubuntu/WSL |
| `frontend` | lint + Vitest + Playwright default (`session` + captures se incluídos no config) + build | CI |
| `frontend-e2e-real` | Sobe jar + Postgres + Redis; `npm run e2e:real` | CI |

Não há job JaCoCo nem threshold. `docs/CHECKLIST_FINAL.md` secção 6.2 P2 e Passo 6 continuam em aberto.

---

## Buracos que importam

Ordem sugerida — o próximo teste que **paga** não é cobertura de DTO.

### P0 — Fecho de caixa

`CloseCashSessionUseCase` e `GetCashSessionClosePreviewUseCase` não têm teste de unidade. `CashSessionControllerTest` mocka o use case e só verifica mapeamento HTTP (400 / 403 / 200).

A regra de tolerância, diferença obrigatória e aprovação pode regressir sem o CI vermelho. Cruza com o code review **H4** (cancelar venda depois do caixa fechado).

### P1 — `JwtAuthFilter`

Os WebMvcTest desligam filtros. Papel 403 está coberto; parse/validação do JWT no filtro não.

### P1 — Dashboard no frontend

`frontend/src/features/dashboard/`: 15 ficheiros de src, 1 teste de domínio (`brandKpis.test.ts`). KPI por marca é o pitch do produto; a UI quase não tem regressão.

### P2 — Gate JaCoCo

Threshold inicial baixo e progressivo (ex.: 50% em `application` + `domain`), para não travar o CI de imediato. Sem isto, a cobertura cai em silêncio.

### P2 — Superfícies menores

`BrandsTab` (0 testes), `NfeProductResolver`, use cases de auth partidos (`Logout` / `Refresh` / `Register`) se alguém refatorar o ficheiro isolado.

Lacunas já apontadas no code review do mesmo dia (não repetir o trabalho aqui, só o mapa de testes):

- Dois XMLs **distintos** sem `chNFe` (C1)
- Cancelamento + comissão (H3)
- Cancelamento com sessão `CLOSED` (H4)
- `@Size(max=128)` no login (H1)

---

## Frontend por feature

| Feature | Src | Testes | Nota |
|---------|-----|--------|------|
| auth | 5 | 2 | Sessão + papéis — sólido |
| orders | 7 | 3 | Parsers |
| sales | 4 | 2 | Tab de venda com stock |
| storefront | 4 | 2 | Totais; páginas em si pouco testadas |
| nfe | 3 | 1 | Tab de importação |
| inventory | 3 | 1 | Parse do ajuste |
| commissions | 2 | 1 | Lista/total |
| dashboard | 15 | 1 | Buraco |
| brands | 1 | 0 | Buraco pequeno |

---

## Próximo passo concreto

1. Teste de unidade de `CloseCashSessionUseCase` (tolerância, razão obrigatória, aprovação, sessão já fechada).
2. Regressão dos buracos C1 / H3 / H4 do code review — cada um é um `@Test` que hoje não existe.
3. JaCoCo no perfil CI com threshold baixo em `com.lojapp.application` e `com.lojapp.domain`.

Não inflacionar cobertura de DTO, entity, repository ou happy path de controller mockado.
