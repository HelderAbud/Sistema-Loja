# Grill — B3 (backup/restore alinhados ao Compose)

**Data:** 2026-06-02  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa B3

## Escopo

Corrigir `backup-postgres-docker.ps1` e `restore-postgres-docker.ps1` que usavam `lojapp/lojapp` fixos enquanto `docker-compose.yml` de dev usa `loja_user` / `loja_db`.

## Perguntas respondidas

| # | Pergunta | Resposta |
|---|----------|----------|
| 1 | Defaults se parâmetro omitido? | Sim — inferidos pelo nome do compose (`prod` → lojapp; caso contrário → loja_*) |
| 2 | Matriz E4 no mesmo PR? | Não — só cabeçalho nos scripts + README + `docs/docker-wsl-ubuntu.md` |

## Decisões

- [x] Módulo partilhado `scripts/Resolve-PostgresDockerParams.ps1`
- [x] Parâmetros `-ComposeFile`, `-DbUser`, `-DbName`, `-Service`
- [x] Ficheiros de backup nomeados `{DbName}-{timestamp}.dump`
- [x] Documentação operacional (README + docker-wsl)

## Aprovado para executar?

- [x] Sim

## Validação

Agente sem `docker` no PATH desta sessão — validação manual no ambiente do utilizador:

```powershell
docker compose -f docker-compose.yml up -d db
.\scripts\backup-postgres-docker.ps1 -ComposeFile docker-compose.yml
.\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\loja_db-<timestamp>.dump -ComposeFile docker-compose.yml
```

Critério: `pg_dump`/`pg_restore` sem erro de role/database inexistente.

## Próximo passo

**B4** — Playwright browser CI = browser config
