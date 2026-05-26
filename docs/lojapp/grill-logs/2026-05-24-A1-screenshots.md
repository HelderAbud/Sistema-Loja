# Grill — A1 (screenshots + GIF)

**Data:** 2026-05-24  
**Participantes:** utilizador + agente  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md`

## Escopo desta sessão

Evidências visuais do portfólio: 6 PNG + 1 GIF em `docs/screenshots/`, README com imagens visíveis.

## Perguntas respondidas

| # | Pergunta | Resposta acordada | Recomendação do revisor |
|---|----------|-------------------|-------------------------|
| 1 | Fluxo do GIF em 30s? | Login → dashboard → vendas/estoque → NFe → dashboard | Alinhado ao `docs/screenshots/README.md` |
| 2 | Conta demo? | `piloto@lojapp.demo` (registar via Swagger se não existir) | Password só em `$env:LOJAPP_SCREENSHOT_PASSWORD`, nunca no Git |
| 3 | Resolução? | 1280×800 (script Playwright) | Mesma janela para todos os PNG |
| 4 | Dados sensíveis? | Usar conta demo; evitar CNPJ/email real de piloto nas capturas | Mascarar se necessário |

## Ferramentas

| Artefato | Uso |
|----------|-----|
| `scripts/capture-portfolio-screenshots.ps1` | PNG 01–06 com API + `npm run dev` |
| Captura manual | GIF `07-fluxo-principal.gif` (< 5 MB) |

## Decisões técnicas

- [x] Nomes de ficheiros = README (`01-login.png` … `06-relatorios.png`)
- [x] PNG gerados (2026-05-26, Playwright + conta `piloto@lojapp.demo`)
- [x] GIF Canva ~18s → `07-fluxo-principal.gif` (renomeado de `.gif.gif`; ~8 MB — comprimir se quiser &lt;5 MB)
- [x] README: PNG + GIF ativos
- [x] `CHECKLIST_FINAL.md` Passo 4 `[x]`

## Aprovado para executar?

- [x] Sim — seguir roteiro abaixo
