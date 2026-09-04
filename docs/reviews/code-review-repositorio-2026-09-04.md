# Code review — repositório completo (LojApp)

> Cópia de trabalho no repo: este ficheiro vive em `docs/reviews/`.

| Campo | Valor |
|--------|--------|
| **Data** | 2026-09-04 |
| **Escopo** | Backend Java/Spring (`src/main`), frontend (`frontend/src`), migrações Flyway, testes e contratos de API |
| **Método** | Revisão estática do código + conferência pontual dos achados graves nos ficheiros citados. Sem alterações de código. Testes não executados neste passe (ambiente Windows sem Docker/Testcontainers). |
| **Prioridade** | Bugs de correção, regressões de comportamento, segurança e testes em falta. Achados primeiro; o resto é contexto. |

Severidade:

- **CRITICAL** — corrompe dados, bloqueia caminho de produto documentado, ou quebra invariante de dinheiro/stock.
- **HIGH** — explorável, relatório/operação errados, ou contrato API partido num ecrã real.
- **MEDIUM** — correção importante, mas com mitigação (API rejeita, isolamento de tenant, aceite MVP).
- **LOW** — higiene, observabilidade, lacuna conhecida.

---

## Resumo

Há **1 CRITICAL** e vários **HIGH** de negócio. Isolamento por `user_id`, XXE na NFe, JWT sem secret default, lock pessimista de stock e idempotência no PDV estão maduros. Os buracos concentram-se em: NFe sem chave, cancelamento incompleto, listagem de venda PDV, contratos de data no storefront, e superfícies de abuso no login.

| Severidade | Qtd. |
|------------|------|
| CRITICAL | 1 |
| HIGH | 14 |
| MEDIUM | 12 |
| LOW | 8 |

**Corrigir primeiro:** C1 (NFe `access_key=""`) → H3/H4 (cancelamento) → H6 (datas `/orders`) → H1 (tamanho da senha no login).

---

## CRITICAL

### C1. NFe sem `chNFe`: `access_key=""` colide no índice único — no máximo uma importação sem chave por loja

- **Onde:** `ImportNfeUseCase.java` (L86–113), `NfeXmlParser.java` (L34), `NfeEntry.java` (L47–48), `V8__nfe_user_access_key_unique.sql` (L7–11), `V16__nfe_content_fingerprint.sql`
- **Bug:** O parser devolve `chNFe` em falta como `""`. O use case grava essa string (`entry.setAccessKey(parsed.accessKey())`) e só preenche `content_fingerprint` quando a chave está vazia. O índice único parcial `uq_nfe_entries_user_id_access_key` aplica-se a `WHERE access_key IS NOT NULL`. String vazia **não é NULL**. A segunda NFe **diferente** sem chave falha com `23505`, mesmo com fingerprints distintos. O índice de fingerprint (V16) nunca chega a proteger o segundo XML porque a inserção morre antes na chave.
- **Trigger:** Importar XML A sem `chNFe`; depois XML B sem `chNFe` (conteúdo diferente). Esperado: sucesso + fingerprint. Obtido: 409 genérico / conflito de unicidade.
- **Impacto:** Caminho documentado em `docs/lojapp/16-nfe-xml-sem-chave-dedup.md` fica inutilizável depois da primeira nota sem chave. Stock da segunda nota não entra.
- **Fix:** Persistir `access_key = null` quando blank. Mapear `DataIntegrityViolation` das constraints `uq_nfe_entries_*` para as exceptions de domínio já existentes.
- **Testes:** Há cobertura do **mesmo** XML sem chave (`LojappCoreServiceTest`). **Não há** teste de dois XMLs distintos sem chave. Este é o teste de regressão obrigatório.

---

## HIGH

### H1. DoS no login: senha sem tamanho máximo + BCrypt strength 12

- **Onde:** `AuthDtos.LoginRequest` (L20); `JwtConfig` (`BCryptPasswordEncoder(12)`); `AuthRateLimitFilter` (60/min/IP)
- **Bug:** `RegisterRequest.password` tem `@Size(min=8, max=128)`. `LoginRequest.password` só tem `@NotBlank`. Endpoint público.
- **Trigger:** `POST /api/v1/auth/login` com `password` enorme. CPU sobe até o rate limit.
- **Fix:** `@Size(max = 128)` (BCrypt usa no máximo 72 bytes) no DTO; rejeitar antes do `matches`.
- **Testes:** Não.

### H2. `ADMIN` lista email e papel de **todas** as lojas

- **Onde:** `UserController.java` (L40–51); `AuthListUsersForAdminUseCase.java` (L21–23, `users.findAll`)
- **Bug:** Operação isolada por `user_id` em todo o resto. `GET /api/v1/users/admin/list` é global. O próprio `UserMeResponse` descreve `ADMIN` com o mesmo isolamento por `user_id`.
- **Trigger:** Conta com `app_role=ADMIN` → enumera PII de todos os tenants.
- **Fix:** Papel de platform-admin separado, ou remover o endpoint, ou filtrar ao próprio `user_id` (inútil). Não usar `findAll` se ADMIN = dono de loja.
- **Testes:** 401/403/200 existem; o vazamento cross-tenant **não**.

### H3. Cancelar venda não estorna comissões

- **Onde:** `CancelSaleUseCase.java` (L40–70); `PosSaleCommissionService.java`; `CommissionReportService.java` (L24–27)
- **Bug:** Repõe stock e marca `cancelledAt`. Não apaga/void `CommissionAccrual`. O relatório lista por `createdAt`, sem olhar para `sale.cancelledAt`.
- **Trigger:** PDV com seller + regra → cancelar → listar/CSV de comissões no intervalo.
- **Fix:** Void/delete dos accruals na mesma TX; filtrar `sale.cancelledAt is null` no report.
- **Testes:** `CancelSaleUseCaseTest` não cobre comissão. `CommissionReportServiceTest` não cobre venda cancelada.

### H4. Cancelar venda depois do fecho de caixa desalinha a reconciliação

- **Onde:** `CancelSaleUseCase.java` (sem check de sessão); `CloseCashSessionUseCase.java` (L55–75); `SalePaymentRepository.sumAmountByCashSession`
- **Bug:** Cancel não bloqueia sessão `CLOSED`. `sumAmountByCashSession` exclui canceladas — o `expectedAmount` **já gravado** no fecho fica mentindo. Stock volta; o caixa fechado não.
- **Trigger:** Finalizar PDV → fechar caixa → `POST /sales/{id}/cancel`.
- **Fix:** Recusar cancel se a sessão estiver fechada; ou exigir reabertura/ajuste auditado.
- **Testes:** Não.

### H5. Listagem de vendas PDV multi-item usa só o header denormalizado

- **Onde:** `CreatePosSaleUseCase.java` (L127–135); `SaleListItemResponse.from` (L21–35); `SaleRepository.searchForUser` (L155–168)
- **Bug:** O header `Sale` guarda só a **1ª linha**. Totais reais estão em `sale_items`. KPI/summary usam items (OK). `GET /sales` mostra produto/qty/preço da primeira linha.
- **Trigger:** PDV com 2+ produtos → histórico / `/orders`.
- **Fix:** Listar a partir de items (ou total agregado + indicador multi-linha). Não tratar o header como a venda inteira.
- **Testes:** Integração cobre stock/items; **não** a listagem multi-item.

### H6. `/orders` envia `YYYY-MM-DD` onde a API exige `Instant` ISO-8601

- **Onde:** `useStorefrontOrdersFilters.ts` + `dateRange.ts` (`toDateInputValue` → `YYYY-MM-DD`); `OrdersPage.tsx` (L76–133); `SaleController.java` (L63–68, `@DateTimeFormat(iso = DATE_TIME)` Instant). Contraste correcto: `SalesHistoryTab.tsx` (L10–20).
- **Bug:** Filtro de datas no storefront manda `from=2026-09-01`. Spring não converte date-only para `Instant` com `ISO.DATE_TIME` → 400. Histórico do piloto faz `toIsoStartOfDay` / `toIsoEndOfDay`.
- **Trigger:** Autenticado; aplicar intervalo de datas em Pedidos.
- **Fix:** Reutilizar a conversão de `SalesHistoryTab` (ou partilhar helper) antes de `listSales` / `summarize*`.
- **Testes:** `dateRange.test.ts` cobre calendário local; **não** o contrato HTTP.

### H7. Série diária de vendas: `date(sold_at)` em UTC

- **Onde:** `SaleRepository.aggregateSalesDaily` (L218–231); `application.yml` (`hibernate.jdbc.time_zone: UTC`)
- **Bug:** Venda 21:00 BRT cai no **dia seguinte** UTC. Loja física brasileira vê o dia errado.
- **Trigger:** Venda entre ~21:00 e 23:59 BRT; comparar `summarizeSalesDaily` com o calendário local.
- **Fix:** `date(s.sold_at AT TIME ZONE 'America/Sao_Paulo')` (ou TZ configurável).
- **Testes:** Não.

### H8. Duplo submit em «Nova venda» sem `Idempotency-Key`

- **Onde:** `PilotoSaleTab.tsx` (L99–137, L278); `frontend/src/api/sales.ts` `registerSale` (não envia header). O PDV do carrinho **já** envia (`CartPage.tsx`). Backend aceita o header (`SaleController` L141–151).
- **Bug:** Botão desliga com `isPending`, mas clique duplo antes do re-render + Enter repetido passam. Sem chave, duas vendas e duas baixas de stock.
- **Trigger:** Duplo clique / Enter enquanto o POST está em voo.
- **Fix:** Gerar `Idempotency-Key` estável por tentativa + `disabled={busy || stockLoading || stockQ.isError}`.
- **Testes:** Há teste de stock na UI; não de idempotência.

### H9. Submit de venda com stock ainda a carregar (ou em erro)

- **Onde:** `PilotoSaleTab.tsx` (L76–82, L111–114, L278); `saleFormParse.ts` (L13–20)
- **Bug:** `isInsufficientStock` só avalia com `stockQty != null`. Enquanto `getProductStock` não resolve, `insufficientStock` é false e o botão fica activo. Backend ainda rejeita 409 — mas a corrida H8 fica pior.
- **Fix:** Bloquear submit se `stockLoading` ou `stockQ.isError`.
- **Testes:** Parcial (excesso com stock já conhecido).

### H10. Catálogo storefront cai para produtos demo quando a API falha **ou** está vazia

- **Onde:** `storefrontShared.tsx` `useStorefrontCatalog` (L64–81)
- **Bug:** `apiProducts.length > 0 ? api : storefrontProducts`. 401/500/rede **e** loja sem produtos mostram catálogo fake (EUR, ratings inventados).
- **Trigger:** Sessão autenticada + `listProducts` a falhar; ou catálogo vazio.
- **Fix:** Se `error`, estado de erro exclusivo. Se vazio, empty state. Nunca misturar demo com sessão real.
- **Testes:** Não.

### H11. Stock do catálogo mapeado para `minimumStock`

- **Onde:** `storefrontShared.tsx` (L46–61); contraste: `ProductStockResponse` / `getProductStock`
- **Bug:** `stock: product.minimumStock` é ponto de reposição, não saldo. UI mente; risco de “vender” o que não há no storefront.
- **Fix:** Usar saldo real ou não mostrar stock.
- **Testes:** Não.

### H12. Fingerprint NFe não é normalizado — bypass de dedupe (e divergência da doc)

- **Onde:** `ImportNfeUseCase.java` (L87); `docs/lojapp/16-nfe-xml-sem-chave-dedup.md` (L5, “XML normalizado”)
- **Bug:** Hash = SHA-256 do `rawXml` byte a byte. Whitespace/`\r\n`/BOM → fingerprints diferentes. **Hoje** o C1 mascara isto (a 2ª importação já rebenta no índice da chave). Depois de corrigir C1 **sem** canonicalizar, o mesmo ficheiro reimportado duplica stock.
- **Trigger:** Reimportar o mesmo XML com newline extra.
- **Fix:** Canonicalizar (trim, EOL, opcionalmente declaração XML) **antes** do hash; alinhar doc e código. Fazer **junto** com C1.
- **Testes:** Ausentes para variantes whitespace.

### H13. Custo do produto não actualiza no match NFe → lucro enviesado

- **Onde:** `NfeProductResolver.java` (L57–73); venda sem `unitCost` usa `product.costPrice`
- **Bug:** Match por EAN/nome reutiliza produto **sem** actualizar custo. `vUnCom` fica só em `nfe_items`. Vendas seguintes herdam custo velho.
- **Trigger:** Produto custo 10 → NFe mesmo EAN custo 20 → venda sem custo explícito.
- **Fix:** Política explícita (último custo / média ponderada) ou exigir custo na venda a partir da NFe.
- **Testes:** `LojappCoreServiceTest` documenta que o custo **não** é sobrescrito no match — comportamento actual, risco de negócio.

### H14. `POST /sales` com caixa aberto não associa `cash_session_id`

- **Onde:** `CreateSaleUseCase.persistSale` (L105–109); contraste: `CreatePosSaleUseCase` (L131)
- **Bug:** Sessão aberta só entra na comissão. A venda “simples” não liga ao caixa nem cria `SalePayment`. Fecho de caixa ignora essas vendas.
- **Trigger:** Abrir caixa → `POST /sales` (tab piloto) → fechar caixa.
- **Fix:** Associar sessão + pagamento, ou proibir `/sales` com caixa aberto (forçar PDV).
- **Testes:** Não.

---

## MEDIUM

### M1. Autorização usa `role` do JWT, não o papel actual na BD

- **Onde:** `JwtAuthFilter.java` (L54–67); `JwtService.parseAccessToken`
- **Bug:** Demover `ADMIN→USER` deixa o access token (~15 min) com `ROLE_ADMIN`.
- **Fix:** Revalidar role em endpoints sensíveis, versionar claims, ou invalidar refresh ao mudar papel.
- **Testes:** Não.

### M2. CSRF de login/registo só corre se já existir cookie de refresh

- **Onde:** `AuthCsrfGuardFilter` — teste `login_withoutRefreshCookie_doesNotApplyCsrfBlock`
- **Bug:** POST `/login` sem cookie não valida Origin. Login CSRF clássico (fixar sessão do atacante no browser da vítima).
- **Fix:** Origin allowlist em todos os POST `/api/v1/auth/**`, ou anti-CSRF explícito.
- **Testes:** Comportamento actual está coberto (é a spec). Residual de ameaça, também em `docs/lojapp/13-threat-model-auth-spa.md`.

### M3. Rate limit de login só por IP; 60/min é folgado

- **Onde:** `AuthRateLimitFilter.java`
- **Fix:** Contador por email normalizado + backoff. Baixar o tecto.
- **Nota:** Já está no backlog residual (`20-backlog-seguranca-residual.md`: CAPTCHA / headers forwarded).

### M4. Refresh: rotação existe; reuse detection de família não

- **Onde:** `AuthRefreshUseCase.java`; backlog `20-backlog-seguranca-residual.md`
- **Bug:** Segundo uso do refresh antigo → 401. Não revoga a sessão se o ladrão usou o token **primeiro**.
- **Fix:** Token families / `deleteByUser_Id` em reuse.
- **Testes:** Rotação feliz coberta em `AuthSessionIntegrationTest`.

### M5. Deploy sem perfil `prod`: Swagger aberto + actuator amplo

- **Onde:** `SecurityConfig.java`; `application.yml` vs `application-prod.yml`
- **Fix:** Fail-closed (swagger off por default). Checklist de deploy já pede `prod`.

### M6. Deadlock potencial em PDV multi-SKU

- **Onde:** `CreatePosSaleUseCase` (locks na ordem das linhas); `InventoryService.loadOrCreateBalanceForUpdate` (L97–119)
- **Bug:** Venda A (P1→P2) vs B (P2→P1) com lock pessimista cruzado.
- **Fix:** Ordenar `productId` antes dos locks.
- **Testes:** `SalesConcurrencyIntegrationTest` cobre 1 SKU, não cruzado.

### M7. Corrida NFe duplicada → 409 genérico

- **Onde:** `ImportNfeUseCase` check-then-insert; `GlobalExceptionHandler` `DataIntegrityViolation`
- **Fix:** Catch pela constraint / re-check após 23505 → `DuplicateNfe*`.
- **Testes:** Só caminho sequencial.

### M8. `ObjectOptimisticLockingFailureException` no cancel → 500

- **Onde:** `Sale.@Version`; `CancelSaleUseCase`; `GlobalExceptionHandler`
- **Fix:** Mapear optimistic lock → 409.
- **Testes:** Não.

### M9. Rotas storefront `/orders`, `/cart`, `/seller` são públicas

- **Onde:** `App.tsx` (L127–158). APIs continuam a exigir JWT (401).
- **Bug:** UI de PDV/financeiro visível sem login; páginas disparam `getCurrentCashSession` / `listSales` anónimas. Não é IDOR se o backend aguentar.
- **Fix:** `ProtectedLayout` + papéis (`canViewFinancialBackoffice` / `canFinalizePosSale`).
- **Testes:** Só `/piloto` está protegido.

### M10. Papel `SELLER` vê PDV na UI; caixa na API não inclui `SELLER`

- **Onde:** `backofficeAccess.ts` (L16–18, POS inclui SELLER); `CashSessionController.java` (L32, `CASHIER,MANAGER,USER,ADMIN`); `PosSaleController` inclui SELLER
- **Trigger:** Login SELLER → `/seller` / checkout → 403 em caixa, 200 em finalize se houver sessão.
- **Fix:** Alinhar matriz (`docs/lojapp/39-matriz-papeis-jwt.md`) com UI e `@PreAuthorize`.

### M11. Ticket médio com filtro de marca distorce vendas multi-marca

- **Onde:** `SaleRepository.aggregateSalesSummary` (L182–204)
- **Bug:** Agrupa por `sale_id` mas filtra items por `brand_id` → média de “tickets parciais”.
- **Fix:** Documentar a métrica ou recalcular (receita da marca / nº de vendas que tocam a marca).

### M12. Retenção de XML impede reaplicar sugestões

- **Onde:** `NfeRetentionService`; `DatabaseNfeRawXmlStorage.retrieve`; `ApplyNfeImportSuggestionsUseCase`
- **Bug:** Job zera `raw_xml`; apply precisa do XML → erro interno.
- **Fix:** 410/404 de domínio, ou persistir metadados da sugestão.

---

## LOW

### L1. `ApiAccessLogFilter` nunca preenche `uid` no MDC

- **Onde:** `ApiAccessLogFilter.java` (L51–61) — testa `principal instanceof Long`; o real é `JwtUser` (`JwtAuthFilter` L59–60)
- **Impacto:** `uid=%X{userId}` no pattern de log fica vazio. Forense fraca, não é vazamento.

### L2. Wildcards `%`/`_` na busca de produtos (só no próprio tenant)

- **Onde:** `ProductSpecifications.nameContainsIgnoreCase`

### L3. `permitAll` amplo em `POST /api/v1/auth/**`

- **Onde:** `SecurityConfig` — POST futuro nesta árvore nasce público.

### L4. Listagem `GET /sales` inclui canceladas; summary/KPI excluem

- **Onde:** `SaleRepository.searchForUser` (sem `cancelledAt is null`)
- **Risco:** Cliente que some a lista erra. Campo `cancelled` existe — UI tem de filtrar.

### L5. Soft-delete de produto deixa saldo órfão fora dos KPIs

- **Onde:** `LojappCatalogService.deleteProduct`; `calcInventoryKpis` ignora `deleted_at`

### L6. Match NFe por nome case-insensitive funde produtos distintos sem EAN

- **Onde:** `NfeProductResolver.java` (L67–69)

### L7. Moeda inconsistente (EUR storefront vs BRL piloto)

- **Onde:** `storefrontShared.formatCurrency` (`pt-PT`/EUR); `SalesHistoryTab` BRL

### L8. Histórico Git com JWT de exemplo queimado

- **Estado:** Working tree exige `${LOJAPP_JWT_SECRET}`; `ProductionSecurityConfig` bloqueia placeholder. Residual se alguém reutilizar o secret antigo (já documentado na Fase A2).

---

## Riscos aceites no MVP (não tratar como bug surpresa)

Estes pontos estão **escritos** em `docs/CONTEXT.md` / comentários de código. Continuam a ser risco residual:

| Tema | Onde | Notas |
|------|------|--------|
| `managerApproval` é auto-declaração | `CloseCashSessionUseCase` L63–67; vocabulário CONTEXT | Sem PIN/segundo actor. Auditoria, não hierarquia. |
| Uma conta = uma loja | CONTEXT | `CASHIER` cancelar vendas da **própria** loja não é IDOR. Granularidade intra-loja está fora do MVP. |
| Sem password reset / MFA | Auth | Ausência de reset reduz superfície; MFA está no backlog `20-backlog-seguranca-residual.md`. |

---

## Lacunas de teste (o que falta para os achados acima)

### Obrigatórios para fechar C/H

| Teste em falta | Protege |
|----------------|---------|
| Dois XMLs distintos sem `chNFe` no mesmo `user_id` | C1 |
| Mesmo XML sem chave com whitespace/BOM diferente | H12 |
| Cancel + accruals (list/CSV não incluem a venda) | H3 |
| Cancel com `CashSessionStatus.CLOSED` → 409 | H4 |
| PDV 2 linhas → `GET /sales` total/produto | H5 |
| `from=YYYY-MM-DD` no FE de `/orders` (ou conversão Instant) | H6 |
| `date(sold_at)` perto da meia-noite UTC vs BRT | H7 |
| Duplo POST `/sales` sem vs com `Idempotency-Key` no piloto | H8 |
| Login com password > 128 chars → 400, sem BCrypt | H1 |
| ADMIN `findAll` vs segundo tenant (PII) | H2 |

### Frontend — módulos grandes sem teste de contrato

- `OrdersPage` / filtros → Instant
- `CartPage` checkout
- `SellerAreaPage`
- `PilotoWorkspacePage` gating por papel
- `useStorefrontCatalog` (demo vs erro)
- `BrandsTab`, `ProductsBrowseTab`, `SalesHistoryTab`
- Idempotência da tab de venda

### Backend — use cases sem unit dedicado

- `CloseCashSessionUseCase` (só controller mockado)
- `GetCurrentCashSessionUseCase` / close preview
- `ApplyNfeImportSuggestionsUseCase`
- `NfeRetentionService`, `S3NfeRawXmlStorage` / `DatabaseNfeRawXmlStorage`
- HTTP IDOR com **dois JWTs** (hoje isolamento é sobretudo no service)

### O que já está bem coberto

- Stock concorrente 1 SKU (`SalesConcurrencyIntegrationTest`)
- Cancel + restore stock
- NFe com chave duplicada + mesmo XML sem chave (caminho feliz do 1º fingerprint)
- XXE no parser (`NfeXmlParserTest`)
- JWT secret / prod gate (`ProductionSecurityConfigTest`)
- Isolamento de catálogo (`CatalogIsolationIntegrationTest`)
- Idempotência de venda/PDV/ajuste (indirecta + use cases)
- Auth session (refresh rotation, logout)

---

## O que foi verificado e está sólido

- **Tenant:** repositórios/use cases usam `findByIdAndUser_Id` / specs `ownedByUser`. Sem concatenação JPQL de input. IDOR óbvio nas queries operacionais **não** encontrado.
- **JWT:** HS256; secret só via env; ≥32 bytes no arranque; refresh opaco hashed (SHA-256); cookie HttpOnly + path `/api/v1/auth` + SameSite=Lax.
- **NFe XXE:** `SecureDocumentBuilderFactory`; teste de DOCTYPE.
- **Upload NFe:** XML no body (não path de ficheiro); chave S3 `prefix/userId/uuid.xml`.
- **Stock:** lock pessimista + `CHECK quantity >= 0` (V7) + retry na criação de balance.
- **Erros 5xx:** mensagem segura (`include-message: never`).
- **CORS:** origins explícitas, sem `*`.
- **Frontend XSS:** sem `dangerouslySetInnerHTML` / `eval`. Access token em memória (Zustand); refresh HttpOnly.
- **Open redirect:** pós-login fixo (`/piloto/products`); sem `returnUrl`.
- **Flyway V1–V23:** sem `TRUNCATE`/`DELETE` em massa. Cascades em `user_id` são apagamento de conta, intencional.
- **CSV de comissões:** prefixo `'` contra formula injection (`CommissionReportService.csvCell`).

---

## Ordem sugerida de correção

1. **C1 + H12 juntos** — `access_key` null + hash canonicalizado + dois testes (XMLs distintos; whitespace).
2. **H3 + H4** — cancelamento transaccional completo (comissão + caixa).
3. **H5 + H14** — contrato de listagem e ligação venda↔caixa.
4. **H6 + H7 + H11 + H10** — storefront deixa de mentir (datas, dia, stock, demo).
5. **H8 + H9** — idempotência e bloqueio de stock na tab piloto.
6. **H1 + H2** — abuso no login e PII ADMIN.
7. Resto MEDIUM/LOW e testes HTTP de IDOR.

---

## Fora de escopo desta revisão

- Não se executou `./mvnw -Pci-unit-tests test` nem `npm test` / e2e neste passe.
- Não se fez pentest dinâmico (ZAP) — item já aberto em `20-backlog-seguranca-residual.md`.
- Não se reescreveu o histórico Git (secret antigo).
- Não se avaliou infra real de produção além do que está no repositório (`application-prod.yml`, compose).
