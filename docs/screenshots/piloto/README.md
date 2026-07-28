# Screenshots do piloto (Dia 12 — E2E)

Evidência do fluxo vertical da trilha: **NFe → estoque → venda**.

Ambiente típico: local (`API :8081` + Vite). Dados fictícios apenas.

| Ficheiro | Conteúdo |
|----------|----------|
| `01-estoque-pos-nfe.png` | Stock / inventário **depois** do import NFe (saldos > 0) |
| `02-venda.png` | Ecrã de venda registada (ou histórico com a linha nova) |
| `03-estoque-pos-venda.png` | Stock do **mesmo** produto com quantidade menor que em `01` |

Referência de isolamento multi-loja (guia junior Parte 1) fica para fatia futura; o Dia 12 fecha só o E2E numa conta demo.

Validação: `docs/lojapp/grill-logs/validation-2026-07-24-trilha-dia-12.md`

## Nota Dia 12 (2026-07-24)

PNG **reais** da conta `piloto-dia12@lojapp.demo` (Playwright + Chrome do sistema).  
API no WSL: no PowerShell, definir proxy antes da captura:

```powershell
$wslIp = (wsl -e hostname -I).ToString().Trim().Split(' ')[0]
$env:LOJAPP_API_PROXY = "http://${wslIp}:8081"
cd frontend
npx playwright test --config=playwright.piloto-dia12.config.ts
```

Password: `.scratch/dia12-pass` ou `/tmp/lojapp-dia12-pass` (não versionar).
