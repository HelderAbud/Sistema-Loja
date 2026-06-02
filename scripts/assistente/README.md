# Scripts — ator B (HTTP reprodutível)

Objetivo: chamadas à API LojApp com credenciais **só em variáveis de ambiente**, alinhado a [docs/lojapp/34-assistente-ia-politica-seguranca.md](../../docs/lojapp/34-assistente-ia-politica-seguranca.md) e [38-pratica-roteiro-local-ator-a.md](../../docs/lojapp/38-pratica-roteiro-local-ator-a.md).

## Ficheiros

| Ficheiro | Descrição |
|----------|-----------|
| `env.example` | Variáveis esperadas; copiar valores para a sessão ou `.env` local (não commitar secrets). |
| `http-roteiro-leitura.ps1` | Smoke **read-only**: produtos, baixo stock, KPIs, stock por produto opcional. |

## Uso rápido (PowerShell)

```powershell
cd <raiz do LojApp>
$env:LOJAPP_BASE_URL = "http://localhost:8000"
$env:LOJAPP_ACCESS_TOKEN = "<jwt após login>"
# opcional: $env:LOJAPP_PRODUCT_ID = "12"
. .\scripts\assistente\http-roteiro-leitura.ps1
```

Mutações (`POST /sales`, `inventory/adjust`, PDV) mantêm-se no roteiro manual ou no doc 38 até existir script com gate explícito.
