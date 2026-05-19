# CHECKLIST FINAL - PRE-ENVIO DO PROJETO

Checklist de auditoria final para publicar o projeto no GitHub e apresentar como portfolio profissional.

## Como usar este checklist

- Status sugerido por item: `NAO INICIADO` -> `EM ANDAMENTO` -> `CONCLUIDO`.
- Prioridade: `P0` (bloqueante), `P1` (muito importante), `P2` (melhoria recomendada).
- Regra de envio: nenhum item `P0` pode permanecer em aberto.

---

## Execucao vertical com agentes de IA (como fechar o checklist)

**Execucao vertical** aqui significa: para cada objetivo do checklist, percorrer **de ponta a ponta** o que esse objetivo exige — codigo, testes, documentacao e evidencia — em **uma fatia coerente**, em vez de espalhar tarefas soltas por camada sem fechar o ciclo.

### Como usar agentes de IA neste fluxo

| Papel (conceitual) | Responsabilidade | Saida esperada |
|--------------------|------------------|----------------|
| **Orquestrador (tu)** | Escolhe o proximo passo do plano sequencial (Fase 1 -> 2 -> 3), aprova escopo e gates sensiveis. | Decisao: qual P0/P1 atacar agora. |
| **Agente planejador** | Quebra o passo em subtarefas verificaveis; aponta arquivos e riscos. | Lista curta de edits + comandos de validacao. |
| **Agente executor** | Implementa o minimo necessario; segue convencoes do repo (`AGENTS.md`, Flyway, contratos). | Diff focado + checklist atualizado quando concluido. |
| **Agente qualidade** | Roda testes, ArchUnit, lint; bloqueia merge se vermelho. | Log de comandos + pass/fail. |
| **Revisor humano** | Obrigatorio em: migrations destrutivas, mudanca de contrato publico da API, segredos, dados sensiveis. | Aprovacao explicita antes de publicar. |

### Ciclo recomendado por item do checklist

1. **Escolher um unico item** (ex.: Passo 5 segredos, ou um P0 da secao 4 Seguranca).
2. **Definir criterio de aceite** copiado do proprio checklist ("Como validar").
3. **Implementar + verificar** com comando real (`mvn test`, perfil `ci-unit-tests` / `ci-integration-tests` conforme `pom.xml`).
4. **Marcar `[x]`** so quando o criterio estiver cumprido (sem marcar por antecipacao).
5. **Seguir para o proximo** na ordem do plano sequencial ate zerar P0.

### Ambiente sem Docker Desktop (Windows)

- Integracoes com **Testcontainers** exigem **Docker acessivel** (ex.: Ubuntu, WSL2 com Docker, ou CI no GitHub).
- No Windows **sem** Docker no PATH: trate integracao como **validar no Ubuntu** ou **no CI**; nao bloqueie o resto do checklist (Passos 1-3, higiene, README) por isso.
- Referencia operacional: `docs/docker-wsl-ubuntu.md` e `AGENTS.md` na raiz do repositorio.

### O que nao delegar aos agentes sem gate

- Rotacao de segredos apos exposicao.
- Apagar ou reescrever historico Git.
- Merge em `main` sem pipeline verde e revisao humana em mudancas sensiveis.

---

## Automatizado vs manual (fechamento do projeto)

### Ja aplicado automaticamente (sem decisao tua)

- **`.gitignore` reforcado:** `application-local.yml` / `application-local.properties` (overrides Spring locais); `id_rsa` / `*.ppk` (chaves comuns acidentais); mantidos `.env`, `backup.sql`, certificados, etc.
- **CI (`.github/workflows/ci.yml`):** o job `repo-hygiene` falha se `.env` estiver versionado (alem de `node_modules`, `backup.sql`, relatorios Playwright).
- **Varredura leve de higiene:** sem `System.out.println` / `printStackTrace` obvios em `src/main/java`; sem `console.log` obvios em `frontend/src` (ultima verificacao por grep).

### Nao feito de proposito (exige decisao ou validacao tua)

- **Passo 6 (JaCoCo):** percentuais minimos de cobertura e politica de falha no CI.
- **Passo 7 (CI):** existem **dois** workflows (`ci.yml` e `backend-ci.yml`); unificar ou manter e escolha de equipa — nao alterado aqui.
- **Remocao ampla de codigo morto / refactors:** so com criterio ou revisao manual.
- **Passo 4 (screenshots/GIF):** depende de capturas reais na tua maquina.

### Sequencia manual recomendada (ordem logica)

1. **Passo 4** — Gerar ficheiros em `docs/screenshots/` e ativar imagens/GIF no `README.md`.
2. **Passo 5** — Confirmar que `.env` / `backup.sql` / `target/` nunca vao em commit nem ZIP; rotacionar segredos se houve exposicao; usar so `.env.example` no Git.
3. **Secao 4.2 + 6.1 (P1)** — Smoke manual (login, CRUD, venda, stock, dashboard) ou repetir no Ubuntu com Docker para integracao.
4. **Secoes 7.1, 8, 9** — Historico Git, deploy alvo, pitch e casos para entrevista (trabalho teu).
5. **Fase 2** — JaCoCo e consolidacao de CI **depois** de decidires thresholds e estrategia de pipeline.
6. **Gate final** — So publicar quando todos os P0 estiverem objetivamente fechados.

### Referencia rapida dos gates em `ci.yml`

- `repo-hygiene` — bloqueia artefactos e `.env` no indice Git.
- `backend-unit` — `./mvnw -Pci-unit-tests test`.
- `backend-integration` — `./mvnw -Pci-integration-tests test` (Docker/Testcontainers no runner).
- `frontend` — `lint`, `test`, `e2e`, `build`.
- `docker-image` — build da imagem + Trivy (conforme workflow).

Workflow adicional: **`backend-ci.yml`** (unit + integracao com servico Postgres). Consolidar com `ci.yml` e opcional (Passo 7).

---

## Analise Consolidada - Especialista 3 e 4

### Especialista 3 (Recrutador Tecnico) - diagnostico de empregabilidade

- **Forcas atuais do projeto**
  - Arquitetura em camadas com contratos em partes importantes da API.
  - Testes relevantes de negocio (idempotencia, concorrencia, isolamento por usuario).
  - Guardrails de arquitetura com ArchUnit.
  - CI com validacao de schema/Flyway para reduzir regressao de banco.
- **Riscos de avaliacao em processo seletivo**
  - Padrao por contratos ainda nao esta 100% uniforme em todos os controllers.
  - Falta gate explicito de cobertura (JaCoCo threshold) no CI.
  - Apresentacao de portfolio pode ficar fraca sem screenshots/demo forte.
- **Leitura de senioridade (referencia)**
  - Júnior: forte.
  - Pleno inicial: competitivo.
  - Senior: ainda sem sinais de lideranca arquitetural completa.

### Especialista 4 (Analista GitHub) - diagnostico de apresentacao e entrega

- **Forcas atuais de repositorio**
  - Checklist robusto, documentacao tecnica e trilha de evolucao.
  - Pipeline de qualidade e testes estruturados.
- **Riscos de percepcao no GitHub**
  - Historico pode perder impacto sem commits organizados por tema.
  - README sem narrativa de valor + evidencias visuais reduz conversao para entrevista.
  - Sem padrao claro de PR/branch, projeto parece menos profissional para recrutador.

---

## Plano Sequencial de Execucao (ordem de importancia)

> Objetivo: executar em ordem cronologica e parar somente quando todos os `P0` estiverem `CONCLUIDO`.

### Fase 1 - Bloqueantes de qualidade (P0)

- [x] **Passo 1 - Fechar padrao de contratos em controllers**
  - **Por que:** elimina inconsistencia arquitetural que pesa em entrevista tecnica.
  - **Como validar:** ArchUnit verde + sem import de servico concreto nos controllers alvo.
- [x] **Passo 2 - Garantir suite critica 100% verde**
  - **Por que:** confianca tecnica antes de publicar.
  - **Como validar:** unitarios, arquitetura e integracao (quando Docker disponivel) sem falhas.
- [x] **Passo 3 - README de portfolio de alto impacto**
  - **Por que:** recrutador decide rapido pelo README.
  - **Como validar:** problema, stack, arquitetura, como rodar, como testar, resultados, roadmap.
- [ ] **Passo 4 - Evidencias visuais obrigatorias (EM ANDAMENTO)**
  - **Por que:** aumenta percepcao de produto real.
  - **Como validar:** screenshots atuais + GIF curto do fluxo principal.
- [ ] **Passo 5 - Seguranca de segredos e ambiente**
  - **Por que:** vazamento de credencial reprova projeto.
  - **Como validar:** nenhum segredo em git + `.gitignore` revisado + envs documentadas.
  - **Automatizado (ja feito):** ver secao **Automatizado vs manual** mais acima neste ficheiro; validacao final e rotacao de segredos continuam **contigo**.

### Fase 2 - Fortalecimento tecnico (P1)

- [ ] **Passo 6 - Adicionar gate de cobertura no CI (JaCoCo)**
  - **Por que:** transforma qualidade em criterio objetivo.
  - **Como validar:** build falha abaixo do threshold definido.
- [ ] **Passo 7 - Consolidar workflows de CI**
  - **Por que:** reduz duplicidade e melhora manutencao.
  - **Como validar:** pipeline unico/claramente segmentado com jobs backend.
- [ ] **Passo 8 - Refinar narrativa para entrevista**
  - **Por que:** saber explicar trade-off vale tanto quanto codar.
  - **Como validar:** pitch de 60-90s + 3 casos de bug/risco mitigado com evidencia.
- [ ] **Passo 9 - Organizar historico Git profissional**
  - **Por que:** historico limpo demonstra maturidade de engenharia.
  - **Como validar:** commits tematicos + conventional commits + branches limpas.

### Fase 3 - Diferenciais de portfolio (P2)

- [ ] **Passo 10 - Melhorias de performance/documentacao operacional**
  - **Por que:** evidencia visao de producao.
  - **Como validar:** doc curta de monitoracao, cache e consultas criticas.
- [ ] **Passo 11 - Plano de evolucao v2.0**
  - **Por que:** mostra pensamento de produto e escalabilidade.
  - **Como validar:** backlog priorizado (curto, medio, longo prazo).

---

## 1. Revisao de Codigo

### 1.1 Higiene e consistencia geral

- [ ] **P0** Remover codigo morto e arquivos de experimento
  - Critero: sem classes/utilitarios nao referenciados no build final.
- [ ] **P0** Remover logs de debug esquecidos
  - Backend: `System.out.println`, logs temporarios e mensagens sem contexto de negocio.
  - Frontend: `console.log` sem finalidade operacional.
- [ ] **P1** Revisar comentarios
  - Remover comentarios obvios, desatualizados ou que contradizem o comportamento atual.
- [ ] **P1** Padronizar nomenclatura
  - Metodos, DTOs e variaveis com semantica de dominio consistente (`sale`, `inventory`, `nfe`, `dashboard`).

### 1.2 Qualidade estrutural de codigo

- [ ] **P0** Revisar imports nao utilizados e warnings de compilacao
  - Critero: build limpo sem warnings evitaveis de codigo local.
- [ ] **P1** Confirmar legibilidade das classes criticas
  - `controller`, `service`, `application use case`, `repository` com responsabilidades claras.
- [ ] **P0** Revisar tratamento de erros
  - Excecoes de dominio mapeadas no handler global com codigo e mensagem coerentes.
- [ ] **P0** Validar regras de negocio criticas
  - Idempotencia, estoque negativo, isolamento por `user_id`, limites de dashboard e fluxo de NFe.

---

## 2. Arquitetura e Organizacao

### 2.1 Camadas e contratos

- [x] **P0** Garantir padrao de dependencia por contratos no backend
  - Controllers devem depender de `service.contract` quando aplicavel.
- [ ] **P1** Validar separacao por camadas
  - `controller -> service/application -> repository -> domain`.
- [x] **P0** Rodar testes ArquUnit e confirmar guardrails
  - Sem dependencia indevida de camadas (ex.: controller acessando repository diretamente).

### 2.2 Organizacao de projeto

- [ ] **P1** Revisar estrutura de pastas e coesao por modulo
  - Pastas `inventory`, `sale`, `nfe`, `dashboard`, `auth` com fronteiras claras.
- [ ] **P2** Identificar arquivos com responsabilidade excessiva
  - Propor plano de extracao sem quebrar contrato publico.
- [ ] **P1** Verificar consistencia backend/frontend
  - Nome de endpoints, DTOs e modelos de exibicao alinhados.

---

## 3. Banco de Dados

### 3.1 Schema e migracoes

- [ ] **P0** Validar cadeia completa de migracoes Flyway
  - Todas as migracoes executam do zero sem ajuste manual.
- [ ] **P0** Confirmar `ddl-auto=validate` em cenarios de integracao/CI
  - Sem depender de `create-drop` para detectar problemas de schema.
- [ ] **P1** Revisar constraints
  - Chaves, unicidade por contexto de loja, integridade referencial.
- [ ] **P1** Revisar indices para consultas de maior uso
  - Vendas por periodo, estoque por produto/usuario, idempotencia por chave/hash.

### 3.2 Dados e operacao

- [ ] **P1** Revisar seeds/dados de demonstracao
  - Dados suficientes para demo funcional sem poluicao de producao.
- [ ] **P1** Validar scripts de backup/restore
  - Execucao testada em ambiente local Docker.
- [ ] **P2** Verificar consistencia de dados antigos
  - Sem registros orfaos e sem violacao de regras de negocio.

---

## 4. Seguranca

### 4.1 Segredos e configuracao

- [ ] **P0** Garantir que segredos nao estao no repositorio
  - `.env`, JWT secret e credenciais apenas por variavel de ambiente.
- [x] **P0** Revisar `.gitignore` para arquivos sensiveis
  - Bloquear chaves, dumps e credenciais locais.
  - Reforco aplicado: overrides Spring locais, chaves SSH comuns; CI bloqueia `.env` versionado.

### 4.2 API e protecao de acesso

- [ ] **P0** Validar autenticacao JWT (access/refresh)
  - Login, refresh e expiracao funcionando como esperado.
- [ ] **P0** Validar autorizacao por papeis
  - Endpoints sensiveis protegidos por perfil correto.
- [ ] **P0** Validar isolamento de dados por usuario
  - Nunca retornar/alterar dados de outra loja.
- [ ] **P1** Confirmar validacao de entrada
  - Bean Validation ativa em DTOs de request.
- [ ] **P1** Confirmar politicas de CORS e rate limiting
  - Configuracao consistente com ambiente alvo.

---

## 5. Performance

### 5.1 Backend

- [ ] **P1** Revisar consultas pesadas
  - Evitar agregacao total em memoria quando ha alternativa paginada no banco.
- [ ] **P1** Confirmar paginacao em endpoints de listagem
  - Produtos, vendas e dashboards com limites definidos.
- [ ] **P2** Revisar uso de cache
  - Chaves, invalidacao e beneficio real de latencia.

### 5.2 Frontend

- [ ] **P2** Revisar renders desnecessarios e chamadas redundantes
  - Monitorar componentes com re-render excessivo.
- [ ] **P2** Otimizar ativos de apresentacao
  - Imagens/screenshots com tamanho adequado para README/portfolio.

---

## 6. Testes

### 6.1 Cobertura funcional

- [x] **P0** Executar suite unitario + arquitetura
  - Critero: sem falhas antes de merge.
- [x] **P0** Executar integracao com Testcontainers (quando Docker disponivel)
  - Vendas, estoque, dashboard, NFe, concorrencia e idempotencia.
- [ ] **P1** Validar fluxos manuais principais
  - Login, CRUD base, venda, baixa de estoque, dashboard.

### 6.2 Confiabilidade

- [ ] **P1** Garantir testes de regressao para pontos alterados recentemente
  - Paginacao de dashboard, idempotencia de ajuste de estoque, contratos de controller.
- [ ] **P1** Confirmar que testes nao dependem de ordem
  - Execucao isolada e reprodutivel.
- [ ] **P2** Definir meta minima de cobertura no CI (JaCoCo)
  - Threshold inicial realista e progressivo.

---

## 7. GitHub Profissional

### 7.1 Historico e branch strategy

- [ ] **P0** Organizar commits por tema
  - Cada commit deve ter objetivo claro e diff revisavel.
- [ ] **P1** Padronizar mensagens em Conventional Commits
  - Ex.: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`.
- [ ] **P1** Limpar branches obsoletas locais/remotas
  - Manter repositorio navegavel para recrutador.

### 7.2 Documentacao de repositorio

- [x] **P0** README principal forte e atualizado
  - Problema, solucao, stack, arquitetura, como rodar, como testar, roadmap.
- [ ] **P0** Incluir evidencias visuais
  - Screenshots atuais e, se possivel, GIF curto do fluxo principal.
  - Estado atual: estrutura e instrucoes prontas no README; faltam arquivos visuais reais em `docs/screenshots/`.
- [x] **P1** Documentar CI/qualidade
  - Quais gates existem e o que eles garantem.
  - Resumo incluido neste ficheiro (secao **Referencia rapida dos gates em `ci.yml`**); detalhe completo nos YAML em `.github/workflows/`.

---

## 8. Deploy e Producao

### 8.1 Preparacao de ambiente

- [ ] **P0** Validar configuracao de producao por variaveis
  - Sem dependencia de valores hardcoded.
- [ ] **P1** Validar backend e frontend em ambiente semelhante ao alvo
  - Build, startup e healthcheck funcionando.
- [ ] **P1** Confirmar banco online com migracoes aplicadas
  - Sem passos manuais ocultos.

### 8.2 Operacao minima

- [ ] **P1** HTTPS/SSL configurado no ambiente publico
  - Obrigatorio para demo compartilhavel.
- [ ] **P2** Revisar logs e monitoracao basica
  - Erros rastreaveis e endpoints de saude operacionais.

---

## 9. Apresentacao para Recrutador

### 9.1 Narrativa tecnica

- [ ] **P0** Preparar pitch de 60-90 segundos
  - Problema de negocio, solucao tecnica e resultado.
- [ ] **P0** Destacar diferenciais reais
  - Testcontainers, Flyway com validate, guardrails ArchUnit, idempotencia.
- [ ] **P1** Explicar decisoes tecnicas e trade-offs
  - O que foi priorizado no MVP e o que ficou para versao seguinte.

### 9.2 Argumentacao para entrevista

- [ ] **P1** Preparar respostas para perguntas previsiveis
  - seguranca, escalabilidade, consistencia de dados, estrategia de testes.
- [ ] **P1** Mapear 3 bugs/risco mitigados
  - antes/depois e evidencia de teste.

---

## 10. Plano Pos-Entrega

### 10.1 Backlog de evolucao

- [ ] **P1** Definir backlog priorizado (curto, medio, longo prazo)
  - Itens orientados a valor de negocio e robustez tecnica.
- [ ] **P2** Registrar refatoracoes planejadas
  - Melhorias arquiteturais sem urgencia imediata.
- [ ] **P2** Definir metas da versao 2.0
  - Escalabilidade, observabilidade avancada, UX e automacoes.

### 10.2 Governanca

- [ ] **P2** Definir cadencia de manutencao
  - rotina de atualizacao de dependencias, revisao de seguranca e hygiene de CI.

---

## Checklist rapido pre-entrega (anti-vazamento)

Executar imediatamente antes de gerar ZIP/partilhar codigo:

- [ ] **P0** Confirmar que `.env` nao sera incluido no pacote.
- [ ] **P0** Confirmar que `backup.sql` nao sera incluido no pacote.
- [ ] **P0** Confirmar que pasta `target/` nao sera incluida no pacote.
- [ ] **P0** Garantir que apenas `.env.example` (sem segredos reais) sera partilhado.
- [ ] **P0** Rever se `LOJAPP_JWT_SECRET` e senha de banco foram rotacionados apos qualquer exposicao.
- [ ] **P1** Gerar ZIP de codigo-fonte com exclusoes ativas (`.env`, `backup.sql`, `target/`).
- [ ] **P1** Testar o ZIP em pasta limpa e validar que sobe sem artefatos sensiveis.

---

## Gate final de publicacao

Publicar no GitHub somente quando:

- [ ] Todos os itens `P0` estao `CONCLUIDO`.
- [ ] Pipeline principal esta verde.
- [ ] README e evidencias visuais estao atualizados.
- [ ] Pitch tecnico esta preparado e ensaiado.

Se qualquer item acima falhar, status do projeto: **NAO PRONTO PARA ENVIO**.

---

## Check final obrigatorio (execucao)

- [ ] Backend rodando
- [ ] Frontend rodando
- [ ] Banco conectado
- [ ] Login funcionando
- [ ] CRUD funcionando
- [ ] Docker disponivel **onde for necessario** (integracao/Testcontainers: Ubuntu/WSL/CI) — no Windows sem Docker Desktop, validar essa linha no ambiente Linux ou no CI
- [ ] Projeto pronto para GitHub
