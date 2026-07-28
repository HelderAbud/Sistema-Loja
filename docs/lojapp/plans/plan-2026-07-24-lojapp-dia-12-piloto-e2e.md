# Plano — Dia 12 piloto E2E (NFe → estoque → venda)

**Data:** 2026-07-24  
**Trilha Helder:** Normal (Fase C)  
**Ambiente:** local (`API :8081` + Vite) — **não** Render

## Objetivo

Provar ponta a ponta: import NFe sintética → stock atualiza → venda baixa saldo, com screenshots em `docs/screenshots/piloto/`.

## Aceite

| Critério | Como |
|----------|------|
| NFe → stock | Quantidade/produto visível após import |
| Venda baixa saldo | Mesmo produto: qty_depois < qty_antes |
| Screenshots | `01-estoque-pos-nfe.png`, `02-venda.png`, `03-estoque-pos-venda.png` |
| Evidência | grill-log + checkboxes em `TRILHA-DIA-A-DIA.md` |

## Fora de escopo

- Seed/NFe em produção Render  
- Isolamento multi-loja (3 contas)  
- `mvn test` / regressão (Dia 13)  
- Commit/PR (só com `PR LojApp`)

## Script

`scripts/dia12-piloto-e2e.sh` — registo/login demo → import NFe → 1 venda → prova saldo.
