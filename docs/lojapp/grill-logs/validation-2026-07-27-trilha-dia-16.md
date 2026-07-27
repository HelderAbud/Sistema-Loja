# Validation — Trilha Dia 16 (README final)

**Data:** 2026-07-27  
**Triagem:** Helder Simple · skills-pessoal fast path  

## Checklist Dia 16 (`TRILHA-DIA-A-DIA.md`)

| Item | Resultado |
|------|-----------|
| Seção Demo com links clicáveis | OK — tabela no topo do README (`## Demo`) com markdown links Vercel/Render/health + piloto |
| Como rodar local (5 linhas) | OK — bloco **Quickstart** em `## Como rodar (local)` |
| Revisão CONTRIBUTING + CHECKLIST_FINAL | OK — criado `CONTRIBUTING.md`; checklist alinhado (Passo 3/7.2/8.x + notas desatualizadas) |

## Alterações desta fatia

- `README.md` — Demo no topo, quickstart, smoke aponta para `#demo`, Documentação com CONTRIBUTING/CHECKLIST/trilha
- `CONTRIBUTING.md` — novo (branch → PR → Principal, autor único, HITL)
- `CHECKLIST_FINAL.md` — revisão documental (sem inventar P0 fechados sem evidência prévia)
- `TRILHA-DIA-A-DIA.md` — Dia 16 marcado
- Este grill-log + plano em `docs/lojapp/plans/`

## Verificação

- Links relativos no README apontam para ficheiros existentes
- Smoke health Render: **timeout ~45s** nesta sessão (cold start free tier) — residual conhecido; URLs documentadas
- Sem `.env` / secrets no diff

## Riscos residuais

- Cold start Render pode falhar health no primeiro hit
- Conta demo pública continua sob HITL
- Postgres free Render: suspensão ~2026-08-16 (já notado na Fase C)
- Dia 17 (LinkedIn) e Dia 18 (revisão trilha) ainda abertos

## Aprovado?

- [x] Fatia documental Dia 16 fechada localmente  
- [ ] Commit/PR — aguarda `PR LojApp`
