# Indice de Prioridades do Sistema (LojApp)

Este indice organiza os documentos de `docs/lojapp` por prioridade de execucao para o sistema:

1. Essencial para rodar/deploy
2. Essencial para desenvolvimento diario
3. Essencial para negocio/produto

Tambem sinaliza lacunas de documentacao e ordem sugerida de leitura/execucao.

## Prioridade A - Rodar e Deploy (obrigatorio)

1. `01-escopo-mvp.md`
   - Define o que entra/nao entra no MVP e o go/no-go tecnico.
   - Ponto critico aberto: validacao de deploy em `prod`.
2. `12-contratos-autenticacao-e-sessao.md`
   - Contrato de auth/sessao (JWT + refresh), erros e limites.
   - Obrigatorio para evitar regressao de login/refresh/logout.
3. `13-estoque-concorrencia-e-idempotencia.md`
   - Invariantes de stock, lock transacional e idempotencia de venda/ajuste.
   - Documento chave para estabilidade de operacao.
4. `16-nfe-xml-sem-chave-dedup.md`
   - Regra de deduplicacao de NFe sem `chNFe`.
   - Protege stock contra importacao duplicada.
5. `15-operacao-continuidade-filas-s3-slo.md`
   - Backup/restore, SLO e alertas para continuidade operacional.
6. `17-versionamento-api-rest.md`
   - Regra para mudancas compativeis e breaking changes.

## Prioridade B - Desenvolvimento Diario (alto impacto)

1. `05-workflow-cursor-superpowers.md`
   - Workflow Cursor + Superpowers + ligacao ao plano Rheyder; usar com `AGENTS.md` e `.cursor/plans/`.
2. `11-checklist-pr-e-convencoes-repositorio.md`
   - Checklist de PR, arquitetura e seguranca para contribuicoes.
3. `31-checklist-producao-prd-lojapp.md`
   - PRD ate merge/QA: comandos LojApp, NFe sintetico vs piloto real. Estado macro do roadmap: `plano-execucao-sprint-1-a-6.md`; portfólio / pré-GitHub: `CHECKLIST_FINAL.md` (raiz do repo).
4. `18-decisoes-e-checklist-entrega.md`
   - Aceite minimo por entrega e registo de decisoes.
5. `14-arquitetura-frontend-por-feature.md`
   - Guia de organizacao frontend por capability (migracao incremental).

**Nota:** documentos numerados `19`, `21`, `22`, `23`, `26`, `27`, `29`, `31-checklist-seguranca-*`, `32-checklist-hardening-*` referenciados em versões antigas do indice **ainda nao estao neste repositório**. Para revisão pre-merge, usar `11` + `18`; para segurança operacional e segredos, `CHECKLIST_FINAL.md` (secções 4 e 8), `12`, `13-threat-model-auth-spa.md` e configs `application-prod.yml` / `docker-compose.prod.yml`.

## Prioridade C - Negocio e Operacao com Pilotos (necessario para escala comercial)

1. `02-pilotos-e-xmls.md`
   - Coleta/validacao de XML real das lojas piloto.
2. `03-implantacao-pilotos.md`
   - Roteiro operacional de implantacao por loja.
3. `04-ativos-comerciais.md`
   - Material comercial e roteiro de demo.

## Seguranca (apoio transversal)

- `13-threat-model-auth-spa.md`
  - Documento de risco e mitigacoes para SPA + auth.
  - Deve ser usado em revisoes de seguranca, sem bloquear fluxo diario.
- `12-contratos-autenticacao-e-sessao.md`
  - Complementa o threat model para sessão, cookies e CORS em piloto/demo.
- `CHECKLIST_FINAL.md` (raiz)
  - Secções **4 (Segurança)** e **10 (anti-vazamento)** — segredos, `.env`, gate antes de partilhar o repo.
- Deploy hardening (quando fores a demo pública): `docker-compose.prod.yml`, `src/main/resources/application-prod.yml`, health/readiness em `AGENTS.md`.

## Lacunas / manutencao de links

- Revisar periodicamente links **internos** entre ficheiros de `docs/lojapp/` (e com o `README.md` na raiz) após renomear ou mover documentos.
- `plano-execucao-sprint-1-a-6.md` existe em `docs/lojapp/`; manter referencias relativas corretas.

## Ordem recomendada de uso no dia a dia

1. Planejar escopo atual: `01`.
2. Por entrega (PRD ate QA): seguir `31-checklist-producao-prd-lojapp.md`; visão macro em `plano-execucao-sprint-1-a-6.md`; portfólio em `CHECKLIST_FINAL.md`.
3. Workflow Cursor/agentes: `05-workflow-cursor-superpowers.md` e plano ativo em `.cursor/plans/` (ex.: piloto Rheyder).
4. Implementar/ajustar backend com contratos: `12`, `13-estoque-concorrencia-e-idempotencia.md`, `16`, `17` (ambiente local: `10-guia-junior-piloto-deploy-proximos-passos.md` secção **6** — Ubuntu/Bash).
5. Validar entrega tecnica: `11`, `18`.
6. Executar prontidao operacional/deploy: `15`.
7. Rodar piloto comercial: `02`, `03`, `04`.

## O que e necessario agora para o sistema (curto prazo)

Checklist objetiva:

- Validar deploy `prod` (pendencia aberta em `01`).
- Fechar coleta de XML real dos pilotos (`02`).
- Revisar consistencia entre ficheiros de `docs/lojapp` apos mudancas de estrutura.
- Garantir rotina de backup/restore testada (`15`).
- Manter controle de contratos API/auth/stock (`12`, `13`, `16`, `17`) em toda mudanca.
