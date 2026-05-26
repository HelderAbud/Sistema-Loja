# Índice técnico unificado (fonte oficial)

Este é o ponto único de entrada para arquitetura, segurança, operação e governança do LojApp.

> **Aviso (2026-05-09):** entradas numeradas `19`, `21`, `22`, `23`, `26`, `27`, `29` e checklists `31-seguranca-dia8` / `32-hardening-dia9` apareciam em versões anteriores deste índice mas **não estão versionadas neste repositório**. O núcleo abaixo lista **apenas** ficheiros presentes em `docs/lojapp/`. Para portfólio, pré-GitHub e gates de segredos, usar `CHECKLIST_FINAL.md` na raiz.

## Núcleo técnico (ficheiros presentes no repo)

- `00-indice-prioridades-sistema.md` — ordem de leitura A/B/C e segurança.
- `05-workflow-cursor-superpowers.md` — workflow Cursor + Superpowers + Rheyder (resumo operacional).
- `11-checklist-pr-e-convencoes-repositorio.md` — higiene de contribuição e critérios de PR.
- `18-decisoes-e-checklist-entrega.md` — registo de decisões e aceite por entrega.
- `17-versionamento-api-rest.md` — política de versionamento de contrato REST.
- `12-contratos-autenticacao-e-sessao.md` — contrato funcional de auth/sessão.
- `13-threat-model-auth-spa.md` — ameaças e mitigação em auth SPA.
- `13-estoque-concorrencia-e-idempotencia.md` — regras críticas de concorrência/idempotência.
- `14-arquitetura-frontend-por-feature.md` — padrão frontend por feature.
- `15-operacao-continuidade-filas-s3-slo.md` — continuidade operacional e SLO.
- `16-nfe-xml-sem-chave-dedup.md` — deduplicação NFe sem chave.
- `31-checklist-producao-prd-lojapp.md` — PRD até merge/QA.

## Planeamento e execução

- `plano-execucao-sprint-1-a-6.md` — macroplaneamento por sprint.
- `10-guia-junior-piloto-deploy-proximos-passos.md` — deploy local, piloto, demo e portfólio.
- `.cursor/plans/` — planos aprovados (ex.: piloto método Rheyder); podem não existir em clones se a pasta não for commitada; ver nota abaixo.

> **Nota (clones do repositório):** roteiros detalhados podem existir em `.cursor/plans/` na tua máquina. Quem clona o projeto deve basear-se **nos `.md` em `docs/lojapp/`**, no `AGENTS.md`, no `CHECKLIST_FINAL.md` e no bloco **Núcleo técnico** acima.

## Estado de documentos legados

Os documentos abaixo permanecem como contexto histórico e comercial, mas não são a única fonte técnica:

- `01-escopo-mvp.md`
- `02-pilotos-e-xmls.md`
- `03-implantacao-pilotos.md`
- `04-ativos-comerciais.md`

Quando houver conflito entre docs, prevalece este índice, o `00-indice-prioridades-sistema.md` e os documentos do bloco **Núcleo técnico** que existirem no disco.

## Assistente / fatias verticais (IA)

- `32-assistente-ia-fatia-vertical-v1.md` — definição de fatia vertical (copiloto/API).
- `33`–`38` — mapeamento API, segurança, roteiro local e observabilidade da fatia.
