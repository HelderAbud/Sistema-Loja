# Índice técnico unificado (fonte oficial)

Este é o ponto único de entrada para arquitetura, segurança, operação e governança do LojApp.

> Para portfólio, pré-GitHub e gates de segredos, usar `CHECKLIST_FINAL.md` na raiz.

## Núcleo técnico (ficheiros presentes no repo)

- `00-indice-prioridades-sistema.md` — ordem de leitura A/B/C e segurança.
- `05-workflow-cursor-superpowers.md` — workflow Cursor + Superpowers + Helder (resumo operacional).
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

## Revisão, operação e entrega (19–30)

- `19-checklist-revisao-senior.md` — checklist de revisão sénior antes de merge.
- `20-backlog-seguranca-residual.md` — backlog de segurança residual pós-MVP.
- `21-go-no-go-deploy-producao.md` — critérios go/no-go para produção.
- `22-observabilidade-rastreabilidade-validacao.md` — observabilidade e rastreio de pedidos.
- `23-riscos-operacionais-matriz.md` — matriz de riscos operacionais.
- `24-matriz-cenarios-e2e.md` — cenários E2E e cobertura de fluxos.
- `25-migracao-frontend-feature-map.md` — mapa de migração `components/` → `features/`.
- `26-performance-baseline-p95-p99.md` — baseline de performance (P95/P99).
- `27-definition-of-done-unico.md` — Definition of Done unificada.
- `29-resumo-executivo-status-riscos-proximos-passos.md` — resumo executivo e próximos passos.
- `30-checklist-handoff-tecnico-junior.md` — handoff técnico para perfil júnior.
- `31-checklist-seguranca-operacional-dia8.md` — segurança operacional (dia 8).
- `32-checklist-hardening-deploy-dia9.md` — hardening e deploy (dia 9).

## Portfólio e entrevista

- `pitch-portfolio.md` — pitch 60–90 s + 3 casos de risco com evidência (testes/docs).
- `grill-logs/2026-06-02-A4-commits-zip.md` — ZIP seguro + auditoria de commits (A4).

## Planeamento e execução

- `plano-execucao-sprint-1-a-6.md` — macroplaneamento por sprint.
- `10-guia-junior-piloto-deploy-proximos-passos.md` — deploy local, piloto, demo e portfólio.
- `.cursor/plans/` — planos aprovados (ex.: piloto método Helder); podem não existir em clones se a pasta não for commitada; ver nota abaixo.

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
- `33-assistente-ia-mapeamento-api-fatia-c.md` — mapeamento API da fatia C.
- `34-assistente-ia-politica-seguranca.md` — política de segurança do assistente.
- `35-assistente-ia-inventario-operacional-e-ator.md` — inventário operacional e ator.
- `36-assistente-ia-contrato-papeis-e-roteiro.md` — contrato de papéis e roteiro.
- `37-assistente-ia-item-6-observabilidade-e-apos.md` — observabilidade e pós-condições.
- `38-pratica-roteiro-local-ator-a.md` — roteiro local prático (ator A).
