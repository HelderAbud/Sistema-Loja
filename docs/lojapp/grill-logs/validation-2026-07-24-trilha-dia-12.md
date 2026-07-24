# Validation — Trilha Dia 12 (Piloto E2E)

**Data:** 2026-07-24  
**Triagem:** Helder Normal  
**Ambiente:** local Ubuntu/WSL (API `127.0.0.1:8000`, Postgres `loja-postgres`) — **não** Render

## Checklist Dia 12

| Item | Resultado |
|------|-----------|
| Health API | OK — `{"status":"UP"}` |
| Fluxo NFe → stock | OK — import `nfe-lote-sintetico-dia7` (3/3 HTTP 200) |
| Venda baixa saldo | OK — produto `13`: qty **2.000 → 1.000** após `POST /sales` (HTTP 200) |
| Screenshots pasta piloto | OK — 3 PNG em `docs/screenshots/piloto/` (ver nota) |
| Plano | OK — `docs/lojapp/plans/plan-2026-07-24-lojapp-dia-12-piloto-e2e.md` |

## Evidência API (sem segredos)

```
REG=200 LOGIN=200 TOKEN_OK
import-nfe: 3 OK, 0 falha(s)
PRODUCT_ID=13 QTY_BEFORE=2.000
SALE_HTTP=200
STOCK_API_AFTER={"quantity":1.000}
E2E_OK: saldo baixou apos venda
```

Conta usada: `piloto-dia12@lojapp.demo` (demo local; password **não** versionada).  
`piloto@lojapp.demo` já existia (409) sem password na sessão — por isso conta nova Dia 12.

## Screenshots — captura UI real ✅

Captura Playwright com **Chrome do sistema** + proxy `LOJAPP_API_PROXY=http://<ip-wsl>:8000` (Windows não vê `localhost:8000` da API no WSL).

| Ficheiro | Conteúdo |
|----------|----------|
| `01-estoque-pos-nfe.png` | Stock (conta `piloto-dia12@lojapp.demo`) |
| `02-venda.png` | Histórico: venda id 5, produto #13, qty 1, 24/07/2026 20:05 |
| `03-estoque-pos-venda.png` | Stock após venda (mesma conta) |

Nota UI: a aba Stock mostra “stock baixo” + ajuste, não a grelha de saldos; a prova visual da venda está em `02-venda.png`. Saldo 2→1 ficou na evidência API acima.

## Scripts

- `scripts/dia12-piloto-e2e.sh`
- `frontend/playwright.piloto-dia12.config.ts` (channel `chrome`)

## Residual

- Dia 13: `mvn test` / CI  
- Commit/PR — aguarda `PR LojApp`

## Aprovado?

- [x] Fatia Dia 12 verificável (E2E API + pasta screenshots preenchida)
- [x] Captura UI real da conta dia12
- [ ] Commit/PR — aguarda pedido explícito
