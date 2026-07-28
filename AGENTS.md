# AGENTS.md — LojApp Pro

Base operacional alinhada a **`Skills/skills/superpowers-cursor-playbook.md`**: contexto claro, plano antes de edições grandes, testes e gate humano em mudanças sensíveis. Guia passo a passo: `docs/lojapp/05-workflow-cursor-superpowers.md`.

> **WSL2 / Ubuntu:** erro de permissão ao usar `docker` ou `docker compose`? Ver [docs/docker-wsl-ubuntu.md](docs/docker-wsl-ubuntu.md).

## Visão geral

- **Produto:** **Plataforma de Gestão Comercial com Automação Fiscal** (LojApp): loja física — NFe, estoque, marca, vendas e KPI por marca.
- **Stack:** Java 21, Spring Boot 3, JPA, Flyway, PostgreSQL, JWT, springdoc-openapi.
- **Isolamento:** `user_id` em todas as entidades operacionais.

## Setup local

### Docker no WSL2 / Ubuntu

Se aparecer `permission denied` ao falar com o Docker daemon, segue o guia de permissões, paths e troubleshooting: [docs/docker-wsl-ubuntu.md](docs/docker-wsl-ubuntu.md).

### Subir Postgres e API

Opcional (Linux/WSL): `bash scripts/docker-wsl-check.sh` confirma acesso ao Docker sem `sudo`.

Na raiz do repositório:

```bash
docker compose up -d
./mvnw spring-boot:run
```

Se tiveres Maven instalado globalmente, podes usar `mvn spring-boot:run` em alternativa ao wrapper (ver tabela abaixo).

## Comandos

| Objetivo | Comando |
|----------|---------|
| Postgres (Docker) | `docker compose up -d` |
| Compilar | `mvn -q -DskipTests package` |
| Subir API | `mvn spring-boot:run` |
| Testes (API) | `mvn test` |
| Testes (frontend) | `cd frontend && npm run test` |
| Lint (frontend, ESLint + Prettier) | `cd frontend && npm run lint` |
| E2E (frontend, Playwright) | `cd frontend && npx playwright install chromium && npm run e2e` |

Swagger local: `http://localhost:8081/swagger-ui.html` (porta por defeito em `application.yml`).

Portas canônicas (portfólio): API **8081**, FE **5173**, Postgres host **5433**, Redis host **6381**. Ver `../Agentes/PORTFOLIO-PORTS.md`.

**Readiness:** antes de considerar o serviço “no ar”, validar `GET http://localhost:8081/actuator/health` (liveness/readiness quando `management.endpoint.health.probes.enabled` está ativo — ver `application.yml`).

## Regras de arquitetura

- Controllers finos; orquestração em `application` (use cases); negócio e serviços de apoio em `service`.
- Schema só via Flyway em `src/main/resources/db/migration/`.
- Integrações externas: preferir cliente/adaptador dedicado (ver `.cursor/rules/backend-java-spring.md`).

## Convenções de código

- Mudanças pequenas e localizadas; reutilizar padrões existentes em `com.lojapp`.
- Evitar abstrações novas sem necessidade clara.
- Não expandir escopo além do pedido; preservar contratos públicos salvo aprovação explícita.

## Testes

- Nova regra de negócio: incluir teste que prove o comportamento.
- Bugfix: teste de regressão quando viável.
- Não concluir com `mvn test` a falhar; se não for possível correr testes, explicar o motivo.

## Workflow (Cursor / Superpowers)

- Tarefa **não trivial**: usar **Plan Mode**, validar plano, depois executar em passos verificáveis.
- Planos relevantes: guardar em `.cursor/plans/` após aprovação.
- Regras persistentes do agente: `.cursor/rules/*.md` (inclui `helder-trilha-diaria.mdc`).

### Continuar a trilha (sem prompt longo)

Fonte: [`TRILHA-DIA-A-DIA.md`](TRILHA-DIA-A-DIA.md) · Helder v1.2 + skills-pessoal (ver regra `helder-trilha-diaria`).

Basta escrever no chat:

```text
Continuar LojApp
```

O agente deve: achar o próximo dia aberto na trilha → triar Helder → aplicar skills → fechar fatia com evidência em `docs/lojapp/grill-logs/`.

Aliases: `Próximo dia` · `Seguir trilha`.

### Abrir PR (sem prompt longo)

Regra: `.cursor/rules/git-autor-unico.mdc`.

Basta escrever no chat:

```text
PR LojApp
```

O agente deve: `Principal` atualizada → branch → commit (só Helder, sem Cursor co-author) → push → PR para `Principal` → **sem merge** → devolver URL + o que conferir.

Aliases: `Abrir PR` · `Abrir PR desta fatia`.

Nunca stage `.env` / secrets.

## Segurança

- Não commitar segredos reais; usar `LOJAPP_JWT_SECRET` (e credenciais DB) só no ambiente.
- Pedir aprovação explícita antes de: migrations destrutivas ou amplas, mudança de contrato API pública, deleções em massa.

### Autor dos commits (portfólio)

- Commits e PRs: **só Helder Abud** — sem `Co-authored-by: Cursor` / `cursoragent` / `Made-with: Cursor`.
- No Cursor: **Settings → Agents → Attribution** (ou **Git & PRs → Attribution**) → desligar Commit e PR Attribution → **reiniciar o Cursor**.
- Dependabot automático: **pausado** em `.github/dependabot.yml` (updates manuais). Histórico antigo no GitHub não some sem rewrite.
- Fluxo: branch a partir de `Principal` → PR → merge só após conferência.
- Gatilho curto: `PR LojApp` (ver secção acima).

## Caminhos importantes

| Caminho | Conteúdo |
|---------|----------|
| `src/main/java/com/lojapp/` | Código da API |
| `src/main/resources/db/migration/` | Scripts Flyway |
| `src/test/java/com/lojapp/` | Testes |
| `docs/docker-wsl-ubuntu.md` | Docker, permissões e WSL2 (troubleshooting local) |
| `docs/lojapp/` | Produto, pilotos, workflow Cursor (`05-workflow-cursor-superpowers.md`); guia operacional: `10-guia-junior-piloto-deploy-proximos-passos.md`; índices: `00-indice-prioridades-sistema.md`, `28-indice-tecnico-unificado.md`; frontend por feature: `14-arquitetura-frontend-por-feature.md`; continuidade/SLO: `15-operacao-continuidade-filas-s3-slo.md`; NFe sem chave: `16-nfe-xml-sem-chave-dedup.md`; versionamento API: `17-versionamento-api-rest.md`; decisões e aceite: `18-decisoes-e-checklist-entrega.md` |
| `TRILHA-DIA-A-DIA.md` | Trilha portfólio (1 dia = 1 fatia); gatilho `Continuar LojApp` |
| `CHECKLIST_FINAL.md` (raiz) | Auditoria portfólio / pré-GitHub (P0/P1); cruza com `.cursor/plans/plan-2026-05-04-helder-piloto-trilha-normal.md` |
| `deploy/prometheus/` | Exemplo de alertas: `alerts.lojapp.example.yml` |
| `scripts/` | Backup/restore Postgres (Docker): `backup-postgres-docker.ps1`, `restore-postgres-docker.ps1`; verificação Docker/WSL: `docker-wsl-check.sh` (bash) |
| `frontend/src/theme/tokens.css` | Tokens CSS globais (cores, raios, sombras, espaçamento); import antes de `App.css` |
| `frontend/src/features/` | Camadas `domain` / `application` / `presentation`; exemplos: `storefront`, `auth`, `orders`, `dashboard`, `sales`, `nfe`, `inventory`; template em `features/_template/README.md` |
| `.cursor/rules/` | Regras por contexto |
| `.cursor/plans/` | Planos aprovados |
