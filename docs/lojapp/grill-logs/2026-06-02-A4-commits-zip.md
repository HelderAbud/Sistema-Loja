# Grill — A4 (commits organizados + ZIP seguro)

**Data:** 2026-06-02  
**Participantes:** utilizador + agente  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa A4

## Escopo

Validar histórico Git legível para portfólio, gerar ZIP de código-fonte **sem segredos** e provar que o pacote abre em pasta limpa.

## Perguntas respondidas

| # | Pergunta | Resposta acordada |
|---|----------|-------------------|
| 1 | Histórico precisa de rebase massivo? | **Não** — fase portfólio já usa PRs com mensagens `feat:` / `fix:` / `docs:` / `chore:` / `test:` (ex.: #14–#17). Reescrever histórico antigo = risco sem ganho. |
| 2 | O que entra no ZIP para recrutador? | Código + docs + scripts; **nunca** `.env`, `backup.sql`, `target/`, `node_modules/`, `.git`, chaves. Incluir `.env.example`. |

## Commits / branches (auditoria 2026-06-02)

Histórico recente em `Principal` (amostra):

- `docs(portfolio): add A3 pitch…` (PR #17)
- `docs(lojapp): version technical docs 19-38…` (PR #16)
- `docs(security): … JWT rotation` (PR #15)
- `fix(ci): …` (PR #14)

**Branches remotas obsoletas:** após merges, apagar localmente `fix/A2-*`, `chore/sync-untracked-docs`, `docs/a3-pitch-portfolio` quando já não forem necessárias.

## ZIP seguro — execução

| Passo | Comando / resultado |
|-------|---------------------|
| Gerar | `scripts/package-source-safe.ps1` → `dist/lojapp-source-safe-a4-test.zip` |
| Verificar conteúdo | `scripts/verify-zip-safe.ps1` → **OK** (529 entradas, zero proibidos) |
| Extrair pasta limha | `dist/zip-verify-extract/` — sem `.env`, sem `target/`, com `.env.example` e `pitch-portfolio.md` |
| Backend | `mvn -Pci-unit-tests test` na pasta extraída → **BUILD SUCCESS** |
| Frontend | `npm ci` OK; `npm run lint` falhou por **Prettier/CRLF** na cópia Windows (mesmo estado que CI Linux resolve) — **gate real = GitHub Actions verde** |

## Melhorias nos scripts

- `package-source-safe.ps1` — exclusões extra (`.local-wip`, `.idea`, `application-local.*`, chaves); validação pré-ZIP do staging.
- `verify-zip-safe.ps1` — **novo** — falha CI/local se ZIP contiver `.env`, `target/`, etc.

## Decisões

- [x] Não commitar ficheiros `.zip` (pasta `dist/` local apenas).
- [x] CHECKLIST secções 7.1 (P0 commits), 9 (Passo 9) e anti-vazamento L396–404 atualizados.
- [x] README já documenta `package-source-safe.ps1`; acrescentada referência a `verify-zip-safe.ps1`.

## Aprovado?

- [x] Sim — A4 fechado para portfólio; seguir **Fase B1** (sanitizar `LojappErrorController`).
