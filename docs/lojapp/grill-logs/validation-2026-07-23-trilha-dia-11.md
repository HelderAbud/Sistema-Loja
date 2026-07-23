# Validation — Trilha Dia 11 (Conta loja fictícia)

**Data:** 2026-07-23  
**Triagem:** Helder Normal (+ HITL)  
**Ambiente:** local Ubuntu (API `localhost:8000`, front `localhost:3000`)

## Checklist Dia 11

| Item | Resultado |
|------|-----------|
| Conta local | OK — Helder registrou conta demo no ambiente local (email pessoal **não** versionado neste log) |
| Seed produtos | OK — `seed-demo-roupas.sh` |
| NFe exemplo | OK — fixtures `scripts/fixtures/nfe-lote-sintetico-dia7` via `import-nfe-folder.sh` |
| Plano piloto | OK — `docs/lojapp/plans/plan-2026-07-23-piloto-demo.md` |

## Notas

- Produção (Render/Vercel) **não** foi usada para seed.  
- Front local precisou de `npm run dev` na porta 3000; API já estava UP.  
- Scripts bash (`export`, `chmod`, `./scripts/*.sh`) devem correr no **Ubuntu**, não no PowerShell.

## Residual

- Dia 12: fluxo NFe → stock → venda + screenshots em `docs/screenshots/piloto/`  
- Preferível no futuro: conta `piloto@lojapp.demo` só para demos (evita email pessoal no fluxo)

## Aprovado?

- [x] Fatia Dia 11 verificável (conta + seed + NFe local)
- [ ] Commit/PR — aguarda `PR LojApp`
