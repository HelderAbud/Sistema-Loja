# Contributing — LojApp (Sistema-Loja)

Guia curto para contribuições humanas e agentes. Detalhe operacional: [`AGENTS.md`](AGENTS.md).

## Fluxo Git

1. Atualizar a base: `git checkout Principal && git pull`
2. Criar branch a partir de `Principal`: `feat/…`, `fix/…`, `docs/…`, `ci/…`
3. Commits em [Conventional Commits](https://www.conventionalcommits.org/) (ex.: `docs(readme): …`)
4. Abrir PR para **`Principal`** — merge só após conferência humana
5. Após merge: `git checkout Principal && git pull` e apagar o ramo local/remoto

Gatilho Cursor: `PR LojApp` (branch → push → PR, **sem** merge automático).

## Autor

- Autor dos commits/PRs: **Helder Abud** apenas
- **Não** incluir `Co-authored-by: Cursor`, `cursoragent` ou `Made-with: Cursor`
- No Cursor: Settings → Agents → Attribution → desligar Commit/PR Attribution

## O que não commitar

- `.env`, secrets, dumps, exports com credenciais
- `frontend/node_modules`, `target/`, backups locais

CI (`repo-hygiene`) falha se `.env` estiver no índice.

## Gates HITL

Pedir aprovação explícita antes de:

- secrets / `LOJAPP_JWT_SECRET` / CORS de produção
- migrations destrutivas ou mudança de contrato API pública
- deploy e commit/push/PR

## Checklist pré-PR

Usar [`CHECKLIST_FINAL.md`](CHECKLIST_FINAL.md) (P0) + diff sem ficheiros sensíveis. Trilha portfólio: [`TRILHA-DIA-A-DIA.md`](TRILHA-DIA-A-DIA.md).
