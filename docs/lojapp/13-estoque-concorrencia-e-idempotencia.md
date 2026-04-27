# Estoque: concorrência, invariantes e idempotência (Sprint 3)

Documento de **desenho e auditoria** alinhado ao código em `InventoryService`, `SalesService`, `ImportNfeUseCase` e repositórios. Serve para onboarding e para evolução controlada (sem “idempotência aparente”).

## 1. Operações que alteram saldo

| Operação | Serviço | Movimento (`InventoryMovementType`) | `source` / `source_id` |
|----------|---------|-------------------------------------|------------------------|
| Venda | `SalesService.registerSale` | `SALE` (delta negativo) | `SALE_REGISTER` / id da venda |
| Entrada NFe | `ImportNfeUseCase.execute` → `increaseFromNfe` | `ENTRY` (positivo) | `NFE_IMPORT` / id do `nfe_entry` |
| Ajuste manual | `InventoryService.adjustStock` | `ADJUSTMENT` (delta livre) | texto do motivo / `null` |

**Cancelamento de venda:** não existe endpoint nem fluxo no código atual; qualquer implementação futura deve **reverter stock** de forma transacional e definir movimento (ex. `SALE_CANCEL`) com regras claras.

## 2. Pontos críticos de corrida (mapeamento)

| Cenário | Risco | Mitigação actual |
|---------|--------|------------------|
| Duas vendas simultâneas no **mesmo produto** (mesmo `user_id`) | Saldo incorrecto se duas leituras + escritas sem sincronizar | `InventoryBalanceRepository.lockByUserAndProduct` com `PESSIMISTIC_WRITE` em `registerStockMovement` → actualização do saldo **serializada por par (utilizador, produto)** |
| Venda + entrada NFe simultâneas no mesmo produto | Idem | Mesmo lock na linha de `inventory_balances` |
| Venda + ajuste simultâneos | Idem | Idem |
| `assertSufficientStock` / leituras de stock **sem** lock | UI ou pré-check pode mostrar saldo **obsoleto** antes de falhar na venda | Documentado no código: a garantia é no `registerStockMovement`; pré-check é **melhor esforço** |
| Import NFe duplicado (mesma chave de acesso) | Duplicar entradas e stock | Verificação `existsByUser_IdAndAccessKey` **antes** de persistir; índice único parcial em BD `(user_id, access_key)` onde `access_key IS NOT NULL` (`V8__nfe_user_access_key_unique.sql`) |
| NFe **sem** chave de acesso preenchida | Duplicar import do “mesmo” XML | Não há chave natural única; **risco residual** — ver secção 6 |

Concorrência entre **produtos diferentes** do mesmo utilizador: transacções distintas, locks em linhas distintas — sem bloqueio cruzado.

## 3. Estratégia de concorrência (por caso)

| Caso | Estratégia | Justificativa |
|------|------------|---------------|
| Actualizar `inventory_balances.quantity` | **Pessimista (`PESSIMISTIC_WRITE`)** | Ponto único de verdade por SKU/loja; fila curta de escritas; evita retry em conflito optimista em pico de vendas |
| Criar linha de saldo inexistente | `saveAndFlush` + captura `DataIntegrityViolationException` + novo `lock` | Duas transacções a criar o mesmo `(user_id, product_id)` — segunda falha na unique e recupera com lock |
| Entidade `Sale` | Campo `version` (`@Version`, migração `V13`) | Útil para **futuras** actualizações à mesma linha (ex. cancelamento); **criação** de venda não depende disto para corrida de stock |

**Lock não** envolve a entidade `Product` na venda — apenas o saldo; dados de produto (preço custo por omissão) são lidos antes do lock de saldo. Alterações raras ao produto durante a venda podem ser aceites como trade-off ou tratadas noutra fase (ex. validar versão de produto).

## 4. Invariantes de domínio (stock)

1. **Saldo não negativo** após movimento, excepto mensagem específica em `ADJUSTMENT` (o código trata insuficiência com `InsufficientStockException` para todos os tipos ao aplicar o delta).
2. **Um movimento** gera **uma** linha em `inventory_movements` e **uma** actualização coerente em `inventory_balances` na mesma transacção do serviço que chama `registerStockMovement`.
3. **Isolamento por `user_id`:** todas as queries de negócio filtram pelo dono (produto, saldo, NFe, venda).
4. **Rastreabilidade:** `source` + `source_id` ligam movimento à venda ou à NFe (ajuste usa motivo em `source`).

## 5. Transacções e ordem (venda vs NFe)

- **Venda:** `SalesService.registerSale` é `@Transactional`: persiste `Sale`, depois `decreaseForSale` → movimento + saldo. Se o saldo falhar, **rollback** da venda.
- **Import NFe:** `ImportNfeUseCase.execute` é `@Transactional`: valida duplicidade de chave, cria `NfeEntry`, por cada linha resolve produto, chama `increaseFromNfe` (movimento + saldo), depois `saveAll` dos `NfeItem`. Falha a meio **reverte** entrada parcial na mesma transacção.

## 6. Idempotência — estado actual vs contrato alvo

### 6.1 O que já existe

- **NFe com `access_key`:** rejeição de duplicado (409 / `DuplicateNfeAccessKeyException`) a nível de aplicação e reforço em BD.
- **Refresh token / auth:** fora do âmbito deste doc.

### 6.2 Lacunas residuais

- **NFe sem chave de acesso:** segundo import pode duplicar stock (sem mudança).
- **Testes de idempotência com cabeçalho:** integração com **PostgreSQL** (`pg_advisory_xact_lock`); perfil de testes H2 em memória **não** cobre o caminho com chave.

### 6.3 Implementação (API)

Cabeçalho opcional **`Idempotency-Key`** (ver `lojapp.idempotency.ttl-hours` e `max-key-length` em `application.yml`).

| Operação | Endpoint | Comportamento |
|----------|----------|----------------|
| Registar venda | `POST /api/v1/lojapp/sales` | Sem cabeçalho: comportamento anterior. Com cabeçalho: gravação em `api_idempotency` (scope `SALE_REGISTER`); replay com **mesmo** corpo devolve o mesmo `SaleCreatedResponse`; corpo diferente → **409 CONFLICT**. |
| Ajuste stock | `POST /api/v1/lojapp/inventory/adjust` | Idem (scope `STOCK_ADJUST`; resposta armazenada como `{}`; segundo `POST` não duplica movimento). |
| Import NFe | (existente) | Continua a usar chave de acesso única por utilizador. |

**Detalhes técnicos:** `ApiIdempotencyService` + lock transaccional PostgreSQL; fingerprint do corpo via `RequestFingerprint`; `CreateSaleUseCase` orquestra a venda; `AdjustInventoryUseCase` orquestra o ajuste (delega o movimento a `InventoryService.applyManualStockAdjustment`).

**Princípio:** após timeout, o cliente deve repetir com a **mesma** chave; extensão futura: estado “em processamento”.

## 7. Duplicidade, retry e timeouts

| Situação | Comportamento com `Idempotency-Key` | Sem chave |
|----------|-------------------------------------|-----------|
| Cliente repete `POST` venda | Uma venda (replay devolve mesma resposta) | Duas vendas possíveis |
| Cliente recebe `200` mas perde a resposta | Replay seguro com mesma chave | Nova venda em novo `POST` |
| `409` NFe chave duplicada | — | Não duplicar stock |
| Mesma chave, corpo alterado | **409 CONFLICT** | — |

## 8. Cache (`@CacheEvict` / KPIs)

KPIs de inventário são invalidados após movimentos. Em cenários extremos, leituras muito paralelas podem ver cache por frações de segundo; o saldo **autoritativo** para decisão de venda está na BD com lock.

## 9. Referências no código

- Lock: `InventoryBalanceRepository.lockByUserAndProduct`
- Movimento + saldo: `InventoryService.registerStockMovement`, `loadOrCreateBalanceForUpdate`
- Venda: `CreateSaleUseCase`, `SalesService.registerSale` (orquestração + listagens)
- Idempotência: `ApiIdempotencyService`, `ApiIdempotency`, migração `V15__api_idempotency.sql`
- NFe: `ImportNfeUseCase.execute`
- Erro stock: `InsufficientStockException`

## 10. Próximos passos

1. NFe sem `access_key`: hash de XML normalizado ou aceitar risco documentado.
2. **Cancelamento de venda** (se existir): movimento inverso, lock no saldo, idempotência no cancelamento.
3. Opcional: expor métricas / limpeza de linhas `api_idempotency` após TTL (hoje o registo expira na leitura e pode ser reutilizado).
