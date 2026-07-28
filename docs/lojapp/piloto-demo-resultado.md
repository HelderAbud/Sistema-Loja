# Resultado do piloto demo (Fase C)

**Período:** 2026-07-23 → 2026-07-26 (trilha Dias 11–15)  
**Objetivo:** provar o fluxo vertical **NFe → estoque → venda** com dados fictícios e evidência reproduzível — sem dados reais de loja.

## O que foi demonstrado

| Passo | Resultado | Evidência |
|-------|-----------|-----------|
| Conta + catálogo demo | Seed local de marcas/produtos + NFe sintética | [Dia 11](grill-logs/validation-2026-07-23-trilha-dia-11.md) |
| E2E stock baixa após venda | Produto #13: qty **2 → 1** após `POST /sales` | [Dia 12](grill-logs/validation-2026-07-24-trilha-dia-12.md) |
| UI autenticada | Screenshots da conta `piloto-dia12@lojapp.demo` | [`docs/screenshots/piloto/`](../screenshots/piloto/) |
| Regressão | Backend 268 testes / 0 falhas; front 37 Vitest OK | [Dia 13](grill-logs/validation-2026-07-25-trilha-dia-13.md) |

## Ambiente usado no piloto

- **Local (WSL/Ubuntu + Windows):** API `:8081`, Postgres Docker `loja-postgres`, Vite `:5173`.  
- **Produção (demo pública):** API [lojapp-api.onrender.com](https://lojapp-api.onrender.com) + front [sistema-loja-psi.vercel.app](https://sistema-loja-psi.vercel.app) — **sem seed do piloto** (HITL deliberado; ver Dia 11).

Scripts úteis (só ambiente local / HITL):

- `scripts/seed-demo-roupas.sh`
- `scripts/import-nfe-folder.sh` (+ fixtures `nfe-lote-sintetico-dia7`)
- `scripts/dia12-piloto-e2e.sh`
- Captura UI: `frontend/playwright.piloto-dia12.config.ts`

## O que **não** entra neste resultado

- Telefones, CNPJ, emails pessoais ou passwords (não versionar; passwords só em `.scratch/` / `/tmp`).  
- XML de NFe real de cliente.  
- Isolamento multi-loja (3 contas) — fica para fatia futura do guia junior.  
- Seed automático na Render.

## Como um recrutador reproduz (resumo)

1. `docker compose up -d` + `./mvnw spring-boot:run` (com `.env` local).  
2. `cd frontend && npm run dev`.  
3. Registar conta demo → seed + import NFe → uma venda.  
4. Ou abrir a demo pública e usar conta já criada no ambiente de deploy (se existir).

Detalhe operacional: [`10-guia-junior-piloto-deploy-proximos-passos.md`](10-guia-junior-piloto-deploy-proximos-passos.md).

## Riscos / follow-up

| Risco | Mitigação |
|-------|-----------|
| Postgres free Render (`lojapp-db`) suspenso ~16/08/2026 | Trocar só host JDBC (Neon/Supabase); API/Vercel mantêm-se — ver estratégia mínima na conversa/deploy |
| Cold start API free | Esperar 1–3 min no primeiro hit após idle |
| Demo prod sem dados | Seed HITL ou dump/reseed quando migrar DB |

## DoD Fase C

- [x] Conta fictícia + seed (Dia 11)  
- [x] Fluxo NFe → stock → venda + screenshots (Dia 12)  
- [x] Regressão verde (Dia 13)  
- [x] Esta página de resultado (Dias 14–15)  
- [x] Validation fase C (`grill-logs/validation-2026-07-26-fase-c-piloto.md`)  
