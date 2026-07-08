# Validation — Trilha Dia 1 (screenshots + stack local)

**Data:** 2026-07-08  
**Plano:** `.cursor/plans/plan-2026-07-08-screenshots-audit.md`  
**Branch proposta:** `docs/trilha-dia-1-portfolio`

## Checklist Dia 1 (TRILHA-DIA-A-DIA.md)

| Item | Resultado |
|------|-----------|
| Screenshots reais em `docs/screenshots/` | OK — 01–06 + extras 08–09 |
| Stack local (Docker + API + front) | OK |
| Login conta demo | OK — `piloto@lojapp.demo` |
| Dados demo (marcas/produtos/vendas) | OK — Ogochi, Hering, 3 SKUs, 2 vendas |
| Plano aprovado salvo | OK |
| Health check | OK — `{"status":"UP"}` |

## Testes automatizados

| Comando | Resultado |
|---------|-----------|
| `./mvnw -q -Pci-unit-tests test` | **BUILD SUCCESS** (exit 0) |
| `cd frontend && npm run test -- --run` | **37/37** pass |

## Screenshots (inventário)

| Ficheiro | Tamanho ~ | Conteúdo |
|----------|-----------|----------|
| `01-login.png` | 82 KB | Login |
| `02-dashboard.png` | 95 KB | KPIs (R$ 349,70) |
| `03-vendas.png` | 84 KB | Histórico 2 vendas |
| `04-estoque.png` | 74 KB | Stock / ajuste |
| `05-importacao-xml.png` | 71 KB | NFe XML |
| `06-relatorios.png` | 81 KB | Catálogo produtos por marca |
| `08-marcas.png` | 64 KB | Extra — Marcas |
| `09-nova-venda.png` | 77 KB | Extra — Nova venda |
| `07-fluxo-principal.gif` | — | **Pendente Dia 2** |

## Correções aplicadas

- `.env` local: comentários bash-safe, BOM removido, `LOJAPP_REGISTRATION_ENABLED=true` (não versionado).
- `.env.example`: comentários sem crases (evita `source` quebrado no WSL).
- `README.md`: GIF 07 comentado até existir ficheiro.
- `scripts/seed-demo-roupas.sh`: seed idempotente demo multimarcas (dev only).

## Riscos residuais

- `06-relatorios.png` usa catálogo produtos; ideal substituir por scroll dashboard ABC (Dia 2).
- GIF README ainda ausente — link desativado propositadamente.
- `.env` nunca commitar.

## Aprovado?

- [x] Fatia Dia 1 verificável localmente
- [ ] Commit/push — aguarda confirmação HITL
