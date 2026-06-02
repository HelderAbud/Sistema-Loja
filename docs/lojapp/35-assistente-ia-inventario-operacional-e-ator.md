# Assistente / agentes — item 4: inventário operacional + ator

**Status:** referência para montar roteiros e prompts (2026-04-30).  
**Base:** [33-assistente-ia-mapeamento-api-fatia-c.md](./33-assistente-ia-mapeamento-api-fatia-c.md), DTOs em `com.lojapp.dto`.

## 1. Ator (quem executa o plano)

| Modo | Quando usar | Prós | Contras |
|------|-------------|------|---------|
| **A — Operador no Cursor (ou IDE) + LLM** | v1 da fatia C com **gate humano** forte | Token só na sessão; cada `POST` pode ser copiado/aprovado explicitamente; alinha ao [34](./34-assistente-ia-politica-seguranca.md). | Menos repetível; depende de atenção humana. |
| **B — Script local** (`curl`, Python, etc.) | Automação repetível em **dev/stage**, CI privado | Reproduzível; fácil injetar `Authorization` e `Idempotency-Key` via env. | Exige disciplina para não commitar segredos; refresh JWT manual ou helper. |
| **C — Orquestrador visual (ex. n8n)** | Várias integrações, agendamento, retries | Visibilidade do fluxo; retries configuráveis. | Mais superfície de configuração; credenciais no nó HTTP. |

**Decisão documentada para o piloto:** começar por **A** (Cursor + humano no loop). Evoluir para **B** quando o roteiro estiver estável. **C** opcional se surgir necessidade de agendamento sem código.

*(Se quiseres fixar outra letra como obrigatória, atualiza esta secção.)*

---

## 2. Cabeçalhos comuns

| Cabeçalho | Obrigatório | Notas |
|-----------|-------------|--------|
| `Authorization: Bearer <access_jwt>` | Sim (exceto `/api/v1/auth/*`) | TTL curto (~15 min); ver [12](./12-contratos-autenticacao-e-sessao.md). |
| `Content-Type: application/json` | Sim em corpo JSON | — |
| `Idempotency-Key: <uuid>` | Recomendado em **toda mutação** suportada | `POST /sales`, `POST /inventory/adjust`, `POST /pos/sales/finalize`. Mesma chave + mesmo corpo → mesma operação (janela documentada na API). |

---

## 3. Formato de erro (referência)

Erros de API alinhados ao handler global usam **`ApiErrorResponse`** (`application/json`):

- `message`, `code` (ex.: `VALIDATION_ERROR`, códigos de domínio em `ApiErrorCode`), `status`, `path`, `timestamp`.

Autenticação/autorização podem devolver o mesmo envelope ou corpo mínimo conforme rota; tratar **401/403** como fim de passo (token expirado ou papel incorreto).

---

## 4. Operações por domínio (corpos e leituras)

### 4.1 Autenticação (pré-requisito do ator)

| Método | Caminho | Body (resumo) |
|--------|---------|----------------|
| POST | `/api/v1/auth/login` | `{ "email", "password" }` → `accessToken` (+ refresh cookie ou fluxo body, ver doc 12). |

O assistente **não** deve guardar password em ficheiro; login é sempre operador ou secret store.

### 4.2 Produtos

| Operação | DTO / resposta | Campos principais (request) |
|----------|----------------|-----------------------------|
| Criar | `ProductRequest` | `name`, `brandId?`, `ean?`, `ncm?`, `sku?`, `costPrice`, `salePrice`, `minimumStock`, `supplierId?`, `productModelId?`, `variantColor?`, `variantSize?` |
| Atualizar | idem + `PUT .../{id}` | Igual |
| Listar | `ProductPageResponse` | Query: paginação, `brandId`, `q`, `lowStock` |

**Exemplo mínimo de criação (JSON):**

```json
{
  "name": "Caderno A4",
  "brandId": 1,
  "costPrice": "5.00",
  "salePrice": "9.90",
  "minimumStock": "10.000"
}
```

### 4.3 Estoque

| Operação | DTO | Notas |
|----------|-----|--------|
| Ajuste | `StockAdjustmentRequest` | `productId`, `quantity` (≠ 0; positivo entrada, negativo saída), `reason` (obrigatório, não vazio, max 500) |
| Saldo produto | `ProductStockResponse` | `quantity` |
| Lista baixo stock | `LowStockResponse` | lista |

**Exemplo de ajuste:**

```json
{
  "productId": 12,
  "quantity": "-2.000",
  "reason": "INVENTARIO_FISICO"
}
```

### 4.4 Venda (backoffice — `USER` / `ADMIN` / `REPRESENTATIVE`)

| Operação | DTO | Campos |
|----------|-----|--------|
| Registar | `SaleRequest` | `productId`, `quantity` (&gt; 0, 3 dec.), `unitPrice`, `unitCost` (opcional — se null, servidor usa custo atual do produto) |
| Resposta | `SaleCreatedResponse` | `id`, `productId`, `quantity`, `unitPrice`, `unitCost`, `soldAt` |

**Exemplo:**

```json
{
  "productId": 12,
  "quantity": "1.000",
  "unitPrice": "19.90",
  "unitCost": "10.00"
}
```

Listagens / resumo / série diária: filtros `from`, `to` (ISO-8601), `productId`, `brandId` — ver Swagger.

### 4.5 PDV (papéis `CASHIER` / `SELLER` / `MANAGER`)

**Sessão de caixa**

| Operação | DTO |
|----------|-----|
| Abrir | `OpenCashSessionRequest`: `openingAmount` (≥ 0) |
| Fechar | `CloseCashSessionRequest`: `cashSessionId`, `countedAmount`, `differenceReason?`, `managerApproval` |

**Finalizar venda**

| Operação | DTO |
|----------|-----|
| Corpo | `PosSaleFinalizeRequest`: `cashSessionId`, `productId`, `quantity`, `unitPrice`, `unitCost?`, `payments` (não vazio) |
| Linha de pagamento | `PosSalePaymentRequest`: `paymentMethod` (`CASH` \| `CARD` \| `PIX`), `amount` (≥ 0.01) |

**Exemplo mínimo de finalize:**

```json
{
  "cashSessionId": 1,
  "productId": 12,
  "quantity": "1.000",
  "unitPrice": "19.90",
  "payments": [
    { "paymentMethod": "CASH", "amount": "19.90" }
  ]
}
```

### 4.6 Dashboard (só leitura)

Sem body: `GET /dashboard/brands`, `GET /dashboard/products-abc`, `GET /dashboard/inventory-kpis` — parâmetros de data onde aplicável (ver 33).

---

## 5. Matriz rápida HTTP (o que preparar no dry-run)

| Situação | Ação do ator |
|----------|----------------|
| 200 / 201 + corpo esperado | Registar no trace da corrida; opcional `GET` de verificação. |
| 400 + `VALIDATION_ERROR` | Corrigir DTO; não retentar o mesmo body. |
| 401 | Renovar token (login/refresh) conforme doc 12. |
| 403 | Papel JWT incorreto para o endpoint (ex.: PDV vs backoffice). |
| 404 | Recurso inexistente ou de outro `user_id`. |
| 409 / conflito de domínio | Ver `message`/`code`; pode exigir intervenção humana. |
| 429 | Respeitar `Retry-After`; não disparar login em loop. |

---

## 6. Checklist do item 4

- [x] Ator definido para o piloto (**A**, com B/C como evolução).
- [x] Inventário com DTOs e exemplos JSON mínimos para mutações da fatia C.
- [x] Cabeçalhos e erros referenciados.

**Próximo item sugerido (fase 5 do plano original):** contrato de mensagens entre papéis (Planeador / Executor / Verificador) ou primeiro roteiro executável passo a passo com gate humano explícito.
