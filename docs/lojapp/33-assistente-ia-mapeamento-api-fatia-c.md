# Assistente / agentes — item 2: API da fatia C (Swagger + testes)

**Status:** mapeamento no código (referência 2026-04-30).  
**Swagger UI local:** `http://localhost:8081/swagger-ui.html` (ver `application.yml` se a porta mudar).

## Âmbito da fatia C (relembrar)

Leitura + escrita com dry-run e gate humano: **produtos**, **stock**, **vendas**, **indicadores (dashboard)**. Fluxo **PDV** (caixa + finalizar venda) usa **papéis diferentes** na API.

**Prefixo comum (LojApp “geral”):** `/api/v1/lojapp`  
**Papéis:** `USER`, `ADMIN`, `REPRESENTATIVE` (via `@PreAuthorize` nos controllers abaixo, salvo PDV).

---

## 1. Produtos

| Método | Caminho | Escrita | Cabeçalhos / notas | Cobertura de testes (referência) |
|--------|---------|--------|---------------------|-----------------------------------|
| GET | `/api/v1/lojapp/products` | Não | Paginação Spring; query: `brandId`, `q`, `lowStock` | `ProductControllerTest`; integração: `CatalogIsolationIntegrationTest` |
| POST | `/api/v1/lojapp/products` | Sim | Body: `ProductRequest` | `ProductControllerTest` |
| PUT | `/api/v1/lojapp/products/{id}` | Sim | Body: `ProductRequest` | `ProductControllerTest` |

**OpenAPI:** `ProductController` tem `@Operation` / `@ApiResponse` no GET e mutações.

---

## 2. Estoque

| Método | Caminho | Escrita | Cabeçalhos / notas | Cobertura de testes (referência) |
|--------|---------|--------|---------------------|-----------------------------------|
| GET | `/api/v1/lojapp/inventory/low-stock` | Não | — | `InventoryControllerTest` (se existir teste dedicado; GETs simples) |
| GET | `/api/v1/lojapp/inventory/products/{productId}/stock` | Não | — | `InventoryControllerTest.productStock_*` |
| POST | `/api/v1/lojapp/inventory/adjust` | Sim | Body: `StockAdjustmentRequest`; opcional `Idempotency-Key` | `InventoryControllerTest`; `InventoryServiceTest`; `AdjustInventoryUseCaseTest`; integração `SalesStockIntegrationTest` |

**OpenAPI:** tag e `bearerAuth` presentes; **falta** `@Operation` (e descrições de resposta) nos três métodos — o Swagger lista o endpoint mas com menos contexto que Produtos/Vendas.

---

## 3. Vendas (registo “geral”)

| Método | Caminho | Escrita | Cabeçalhos / notas | Cobertura de testes (referência) |
|--------|---------|--------|---------------------|-----------------------------------|
| GET | `/api/v1/lojapp/sales` | Não | Filtros: `from`, `to`, `productId`, `brandId`, página | `SaleControllerTest` (só listagem) |
| GET | `/api/v1/lojapp/sales/summary` | Não | Mesmos filtros temporais / produto / marca | **Sem** `SaleControllerTest` dedicado (risco de regressão só em serviço/integração) |
| GET | `/api/v1/lojapp/sales/daily` | Não | Idem | Idem |
| POST | `/api/v1/lojapp/sales` | Sim | Body: `SaleRequest`; opcional `Idempotency-Key` | `CreateSaleUseCaseTest`; integração `SalesStockIntegrationTest`, `SalesConcurrencyIntegrationTest`; **não** há teste MockMvc em `SaleControllerTest` para POST |
| POST | `/api/v1/lojapp/sales/{id}/cancel` | Sim | — | Domínio / integração possível; **não** visto em `SaleControllerTest` |

**OpenAPI:** tag presente; vários métodos **sem** `@Operation` rico (comparado com `ProductController`).

---

## 4. Dashboard (KPIs / leitura)

| Método | Caminho | Escrita | Notas | Cobertura de testes (referência) |
|--------|---------|--------|--------|-----------------------------------|
| GET | `/api/v1/lojapp/dashboard/brands` | Não | `from`, `to`, `brandLimit`, `brandOffset` | `DashboardControllerTest`; `DashboardLoadIntegrationTest` |
| GET | `/api/v1/lojapp/dashboard/products-abc` | Não | `from`, `to` | Idem |
| GET | `/api/v1/lojapp/dashboard/inventory-kpis` | Não | — | Idem |

---

## 5. PDV (venda + caixa) — papéis `CASHIER`, `SELLER`, `MANAGER` (vendas) / `CASHIER`, `MANAGER` (caixa)

Quem implementar o assistente para **mesmo fluxo que o PDV** precisa de **JWT com estes papéis**, não só `USER`.

**Vendas PDV**

| Método | Caminho | Escrita | Cabeçalhos / notas | Cobertura de testes (referência) |
|--------|---------|--------|---------------------|-----------------------------------|
| POST | `/api/v1/lojapp/pos/sales/finalize` | Sim | Body: `PosSaleFinalizeRequest`; `Idempotency-Key` documentada como importante | `PosSaleControllerTest`; `SalesStockIntegrationTest` |

**Caixa**

| Método | Caminho | Escrita | Notas | Cobertura de testes (referência) |
|--------|---------|--------|--------|-----------------------------------|
| GET | `/api/v1/lojapp/pos/cash-sessions/current` | Não | — | `CashSessionControllerTest` |
| GET | `/api/v1/lojapp/pos/cash-sessions/{id}/close-preview` | Não | Query `countedAmount` opcional | Ver `CashSessionControllerTest` |
| POST | `/api/v1/lojapp/pos/cash-sessions/open` | Sim | Body: `OpenCashSessionRequest` | Idem |
| POST | `/api/v1/lojapp/pos/cash-sessions/close` | Sim | Body: `CloseCashSessionRequest` | Idem |

---

## 6. Lacunas (histórico) — itens 8 e 9 tratados no código

| Área | Estado |
|------|--------|
| Swagger | `InventoryController` e `SaleController`: `@Operation` / `@ApiResponse` adicionados (revisão contínua no Swagger UI). |
| Testes WebMvc | `SaleControllerTest`: cobre `summary`, `daily`, `POST /sales` (com `Idempotency-Key`), `cancel`. |
| Papéis | Assistente “uma só credencial” pode **não** cobrir PDV; documentar dois modos: **backoffice** (`USER`/`ADMIN`/…) vs **PDV** (`CASHIER`/…). |
| Idempotência | `POST /sales` e `POST /inventory/adjust` e PDV `finalize`: agente deve sempre planear `Idempotency-Key` em mutações (ver descrições nos controllers). |

---

## 7. Fecho do item 2 (checklist)

- [x] Endpoints da fatia C inventariados (leitura + escrita + PDV opcional).
- [x] Swagger: Estoque e Vendas alinhados com anotações OpenAPI nos controllers (ver itens 8–9 em [37](./37-assistente-ia-item-6-observabilidade-e-apos.md)).
- [x] Testes críticos: integração forte + `SaleControllerTest` alargado (summary, daily, registo, cancel).

**Próximo item sugerido na sequência:** política de segurança (JWT, ambientes, segredos) + escolha do “ator” (script vs ferramenta), depois inventário operacional fino (corpos DTO exemplo a exemplo).
