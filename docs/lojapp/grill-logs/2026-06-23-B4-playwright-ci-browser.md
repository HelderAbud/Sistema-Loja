# Grill — B4 (Playwright: browser CI = browser config)

**Data:** 2026-06-23
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa B4
**Branch:** `fix/B4-playwright-ci-browser`

## Escopo

Garantir que o browser usado pelo Playwright em CI bate com o browser instalado no workflow: a CI instala **Chromium**, mas a config podia usar **Microsoft Edge**, causando E2E vermelho só no GitHub.

## Perguntas respondidas

| # | Pergunta | Resposta |
|---|----------|----------|
| 1 | A config ainda usa Edge fixo? | Não — `playwright.config.ts` já usa condicional `process.env.CI ? chromium : msedge` |
| 2 | A CI instala o browser certo? | Sim — `ci.yml` job `frontend` roda `npx playwright install --with-deps chromium` antes de `npm run e2e` |
| 3 | A branch B4 precisa de mudança de código? | Não — `git diff Principal...fix/B4-playwright-ci-browser` = vazio |
| 4 | Quando a correção entrou? | Commit `0554d67` (`fix(ci): align new integration and e2e tests with CI constraints`) |
| 5 | Local continua a usar Edge? | Sim — fora de CI usa `msedge` (Desktop Edge), sem exigir Chromium instalado |

## Decisões

- [x] Não alterar código: o objetivo do B4 já está satisfeito por commit anterior.
- [x] Registar o achado neste grill-log (evidência de que a tarefa foi auditada, não esquecida).
- [x] Manter a branch para o fecho documental; merge/cleanup fica com o humano (HITL).

## Evidência

Config alinhada (`frontend/playwright.config.ts`):

```ts
projects: [
  process.env.CI
    ? { name: "chromium", use: { ...devices["Desktop Chrome"] } }
    : { name: "msedge", use: { ...devices["Desktop Edge"], channel: "msedge" } },
],
```

CI alinhada (`.github/workflows/ci.yml`, job `frontend`):

```yaml
npx playwright install --with-deps chromium
npm run e2e
```

Verificação local de E2E não aplicável a este item: localmente a config usa `msedge`, que **não** é o caminho que o B4 corrige (CI/Chromium). A prova relevante é o run verde do job `frontend` no GitHub Actions.

## DoD

- [ ] **CI frontend verde** — confirmar no GitHub Actions (job `frontend` do workflow `CI`) no último push da branch / `Principal`. `gh` não autenticado nesta sessão; confirmação fica com o humano.
- [x] **Grill log curto** — este ficheiro.

## Próximo passo

- Confirmar CI verde no GitHub e fazer merge da branch `fix/B4-playwright-ci-browser` (ou descartá-la, já que o diff é vazio).
- Fase B concluída → avançar para **Fase C / PR1** (conectar `AdjustInventoryUseCase`).
