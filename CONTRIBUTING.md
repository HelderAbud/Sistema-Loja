# Contributing — LojApp (Sistema-Loja)

Guia curto para contribuições humanas e agentes. Detalhe operacional: [`AGENTS.md`](AGENTS.md).

## Fluxo Git

1. Atualizar a base: `git checkout main && git pull`
2. Criar branch a partir de `main`: `feat/…`, `fix/…`, `docs/…`, `ci/…`
3. Commits em [Conventional Commits](https://www.conventionalcommits.org/) (ex.: `docs(readme): …`)
4. Abrir PR para **`main`** — merge só após conferência humana
5. Após merge: `git checkout main && git pull` e apagar o ramo local/remoto

Gatilho Cursor: `PR LojApp` (branch → push → PR, **sem** merge automático).

## Autor

- Autor dos commits/PRs: **Helder Abud** apenas
- **Não** incluir `Co-authored-by: Cursor`, `cursoragent` ou `Made-with: Cursor`
- No Cursor: Settings → Agents → Attribution → desligar Commit/PR Attribution

## O que não commitar

- `.env`, secrets, dumps, exports com credenciais
- `frontend/node_modules`, `target/`, backups locais

CI (`repo-hygiene`) falha se `.env` estiver no índice.

## Dependabot

- PRs de versão: **limit 1** por ecossistema (Maven + npm), agenda mensal — ver [`.github/dependabot.yml`](.github/dependabot.yml).
- Revisar e mergear manualmente (autor Helder); não merge automático.
- Pipeline canónica: [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Gates HITL

Pedir aprovação explícita antes de:

- secrets / `LOJAPP_JWT_SECRET` / CORS de produção
- migrations destrutivas ou mudança de contrato API pública
- deploy e commit/push/PR

## Checklist pré-PR

Usar [`docs/CHECKLIST_FINAL.md`](docs/CHECKLIST_FINAL.md) (P0) + diff sem ficheiros sensíveis. Trilha portfólio: [`docs/TRILHA-DIA-A-DIA.md`](docs/TRILHA-DIA-A-DIA.md).
