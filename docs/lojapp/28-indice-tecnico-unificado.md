# Índice técnico unificado (fonte oficial)

Este é o ponto único de entrada para arquitetura, segurança, operação e governança do LojApp.

## Núcleo técnico (usar sempre)

- `11-checklist-pr-e-convencoes-repositorio.md` — higiene de contribuição e critérios de PR.
- `27-definition-of-done-unico.md` — critério canónico de pronto para entrega (backend + frontend).
- `18-decisoes-e-checklist-entrega.md` — registo de decisões e aceite por entrega.
- `17-versionamento-api-rest.md` — política de versionamento de contrato REST.
- `12-contratos-autenticacao-e-sessao.md` — contrato funcional de auth/sessão.
- `13-threat-model-auth-spa.md` — ameaças e mitigação em auth SPA.
- `13-estoque-concorrencia-e-idempotencia.md` — regras críticas de concorrência/idempotência.
- `14-arquitetura-frontend-por-feature.md` — padrão frontend por feature.
- `15-operacao-continuidade-filas-s3-slo.md` — continuidade operacional e SLO.
- `21-go-no-go-deploy-producao.md` — checklist de deploy.
- `22-observabilidade-rastreabilidade-validacao.md` — validação de observabilidade.
- `23-riscos-operacionais-matriz.md` — risco operacional (impacto x probabilidade).
- `26-performance-baseline-p95-p99.md` — evidências de desempenho (p95/p99).

## Planeamento e execução

- `plano-execucao-sprint-1-a-6.md` — macroplaneamento por sprint.
- `00-indice-prioridades-sistema.md` — ordem de leitura e prioridades (A/B/C).
- `10-guia-junior-piloto-deploy-proximos-passos.md` — deploy local, piloto, demo e portfólio.
- `29-resumo-executivo-status-riscos-proximos-passos.md` — status, riscos e próximos passos (visão condensada).

> **Nota (clones do repositório):** roteiros muito detalhados (ex.: “plano 14 dias”, automações com agente) podem existir só em `.cursor/plans/` na tua máquina — pasta **ignorada pelo Git**. Quem clona o projeto deve basear-se **nestes `.md`** e no bloco **Núcleo técnico**, não em paths dentro de `.cursor/`.

## Estado de documentos legados

Os documentos abaixo permanecem como contexto histórico e comercial, mas não são fonte técnica principal:

- `01-escopo-mvp.md`
- `02-pilotos-e-xmls.md`
- `03-implantacao-pilotos.md`
- `04-ativos-comerciais.md`

Quando houver conflito, prevalece este índice e os documentos do bloco "Núcleo técnico".
