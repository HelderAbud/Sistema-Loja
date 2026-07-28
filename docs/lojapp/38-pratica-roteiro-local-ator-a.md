# Prática — roteiro §5 em local com ator A (Cursor + humano)

**Objetivo:** executar **uma vez** o roteiro backoffice de [36 §5](./36-assistente-ia-contrato-papeis-e-roteiro.md), com `runId`, aprovações explícitas e **sem** gravar JWT/password em ficheiros versionados.

**Onde guardar o trace:** pasta **`local/agent-runs/`** na raiz do repo (cria se não existir). Está em [`.gitignore`](../../.gitignore) — **não** será commitada.

---

## 1. Pré-requisitos

1. Postgres: `docker compose up -d` (na raiz do LojApp).
2. API: `./mvnw spring-boot:run` (ou `mvn spring-boot:run`) — porta por defeito **8081** (ajusta `BASE` se mudares).
3. Utilizador com papel **USER** / **ADMIN** / **REPRESENTATIVE** e dados reais (produto com stock suficiente para uma venda de teste).
4. Ferramenta: **Swagger UI** opcional para validar contratos: `http://localhost:8081/swagger-ui.html`.

---

## 2. Gerar `runId` e ficheiro de trace

No PowerShell:

```powershell
New-Guid   # copiar para o trace
```

Cria o ficheiro (exemplo de nome):

`local/agent-runs/corrida-2026-04-30-runId.txt` ou `.md`

Usa o **template da secção 8** — preenche só **status codes**, **ids**, **quantidades**, **aprovações**; **nunca** colas o valor de `accessToken` no ficheiro (podes escrever “token só em `$env:LOJAPP_ACCESS_TOKEN` nesta sessão”).

---

## 3. Login (operador) — token só em memória

Substitui email/password no terminal; **não** copies o JSON de resposta para o trace completo.

```powershell
$BASE = "http://localhost:8081"
$loginBody = '{"email":"TEU_EMAIL","password":"TEU_PASSWORD"}'
$r = Invoke-RestMethod -Uri "$BASE/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json; charset=utf-8"
$env:LOJAPP_ACCESS_TOKEN = $r.accessToken
```

Confirmação no trace: `login 200, token em $env:LOJAPP_ACCESS_TOKEN` (sem colar o token).

---

## 4. Leituras (passos 2–4)

**Produtos (escolher `productId`):**

```powershell
$h = @{ Authorization = "Bearer $env:LOJAPP_ACCESS_TOKEN" }
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/products?page=0&size=20" -Headers $h
```

**Stock baseline (substituir `PRODUCT_ID`):**

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/inventory/products/PRODUCT_ID/stock" -Headers $h
```

**Opcional — KPIs:**

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/dashboard/inventory-kpis" -Headers $h
```

Anota no trace: `productId`, `quantity` antes da venda.

---

## 5. Gate humano — `HttpCallProposal` (passo 5)

Antes de qualquer `POST`, no Cursor (ator A):

1. Pede ao modelo um **`HttpCallProposal`** alinhado a [35](./35-assistente-ia-inventario-operacional-e-ator.md) (`SaleRequest`: `productId`, `quantity`, `unitPrice`, `unitCost` opcional).
2. Gera um **`Idempotency-Key`** novo (ex.: novo GUID) e regista-o no trace.
3. **Aprovação explícita:** escreve no trace `HumanApproval: approve` + data + as tuas iniciais.

Exemplo de corpo JSON (ajusta números ao teu catálogo):

```json
{
  "productId": 1,
  "quantity": "1.000",
  "unitPrice": "9.90",
  "unitCost": "5.00"
}
```

---

## 6. Executar venda (passo 6)

**Recomendado:** colar o body numa variável para não perder precisão decimal.

```powershell
$IDEMP = [guid]::NewGuid().ToString()
$body = @'
{"productId":1,"quantity":"1.000","unitPrice":"9.90","unitCost":"5.00"}
'@
$hdr = @{
  Authorization = "Bearer $env:LOJAPP_ACCESS_TOKEN"
  "Idempotency-Key" = $IDEMP
  "Content-Type" = "application/json; charset=utf-8"
}
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/sales" -Method Post -Headers $hdr -Body $body
```

Regista no trace: `Idempotency-Key`, `sale id` devolvido, `statusCode` (200).

---

## 7. Verificação (passos 7–8)

**Stock após venda:**

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/inventory/products/PRODUCT_ID/stock" -Headers $h
```

**Listagem de vendas (opcional):**

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/sales?productId=PRODUCT_ID&page=0&size=5" -Headers $h
```

Preenche **`VerificationReport`** no trace: `pass: true/false`, evidência (quantidade antes/depois).

---

## 8. Template mínimo do trace (copiar para `local/agent-runs/`)

```markdown
# Corrida assistente — ator A

- runId:
- environment: local
- startedAt:
- endedAt:
- jwtRole: USER (ou o que usaste)
- BASE: http://localhost:8081

## Plano
- [ ] Passo 1 login
- [ ] Passo 2–4 leituras
- [ ] Passo 5 proposal + HumanApproval (approve / reject)
- [ ] Passo 6 POST /sales
- [ ] Passo 7–8 verificação

## Registo (sem tokens)
| Step | Método | Path | Status | Notas |
|------|--------|------|--------|--------|
| | POST | /api/v1/auth/login | | token só em env |
| | GET | /api/v1/lojapp/products | | productId escolhido: |
| | GET | .../inventory/products/{id}/stock | | qty antes: |
| | POST | /api/v1/lojapp/sales | | Idempotency-Key: ; sale id: |
| | GET | .../stock | | qty depois: |

## HumanApproval
- decision: approve
- approvedBy:
- approvedAt:
- notes: (ex.: confirmei preço e produto)
```

---

## 9. Depois da corrida

- Limpa o token da sessão se partilhas o PC: `Remove-Item Env:LOJAPP_ACCESS_TOKEN`.
- Se quiseres evidência no repo **sem** dados sensíveis, podes commitar só um **resumo** em doc (ex.: “corrida concluída em local, runId X, verificação OK”) — nunca o trace bruto com headers.

---

## Referências

- Contrato e roteiro: [36](./36-assistente-ia-contrato-papeis-e-roteiro.md)  
- DTOs: [35](./35-assistente-ia-inventario-operacional-e-ator.md)  
- Observabilidade: [37](./37-assistente-ia-item-6-observabilidade-e-apos.md)
