# Validation — Trilha Dia 6 (preparação deploy)

**Data:** 2026-07-16  
**Triagem:** Helder Normal  
**Plano:** `docs/lojapp/plans/plan-2026-07-16-deploy-railway.md` (cópia; `.cursor/plans/` local ignorado)

## Checklist Dia 6

| Item | Resultado |
|------|-----------|
| Ler guia deploy (Parte 4) | OK — vars `prod`, CORS, JWT, datasource |
| Plano `plan-2026-07-16-deploy-railway.md` | OK |
| Checklist env documentado | OK — JWT, Postgres, CORS, VITE (Dia 8) |
| HITL gerar secrets | OK — Helder confirmou “secrets ok” (valores só no `.env` local) |

## Autor / bots (pedido portfólio)

| Ação | Status |
|------|--------|
| Regra local: sem `Co-authored-by: Cursor` | OK (`.cursor/rules/git-autor-unico.mdc`) |
| Hook local `prepare-commit-msg` | OK (strip trailers Cursor) |
| Dependabot PRs automáticos | Pausado (`updates: []` em `dependabot.yml`) |
| Histórico antigo (cursoragente / dependabot) | Mantém-se — limpar exigiria rewrite; **não** feito |

## Alterações desta fatia

- `.github/dependabot.yml` — pausa updates automáticos
- `AGENTS.md` — política autor único + nota Attribution Cursor
- `TRILHA-DIA-A-DIA.md` — Dia 6 marcado (HITL secrets aberto)
- Plano deploy + este grill-log
- (local, não versionado) regras `.cursor/` + hook `.git/hooks/`

## Riscos residuais

- Secrets ainda não gerados — Dia 7 bloqueado até HITL.
- Pausar Dependabot = updates manuais; alerts de segurança no GitHub ainda úteis.
- Attribution no Cursor Settings (UI) deve ser desligada pelo Helder + restart.

## Aprovado?

- [x] Fatia documental Dia 6 verificável
- [x] Secrets gerados (HITL)
- [ ] Commit/PR — após confirmação; mensagem **sem** Co-authored-by
