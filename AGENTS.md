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

Swagger local: `http://localhost:8000/swagger-ui.html` (porta por defeito em `application.yml`).

**Readiness:** antes de considerar o serviço “no ar”, validar `GET http://localhost:8000/actuator/health` (liveness/readiness quando `management.endpoint.health.probes.enabled` está ativo — ver `application.yml`).

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
- Regras persistentes do agente: `.cursor/rules/*.md`.

## Segurança

- Não commitar segredos reais; usar `LOJAPP_JWT_SECRET` (e credenciais DB) só no ambiente.
- Pedir aprovação explícita antes de: migrations destrutivas ou amplas, mudança de contrato API pública, deleções em massa.

## Caminhos importantes

| Caminho | Conteúdo |
|---------|----------|
| `src/main/java/com/lojapp/` | Código da API |
| `src/main/resources/db/migration/` | Scripts Flyway |
| `src/test/java/com/lojapp/` | Testes |
| `docs/docker-wsl-ubuntu.md` | Docker, permissões e WSL2 (troubleshooting local) |
| `docs/lojapp/` | Produto, pilotos, workflow Cursor; guia operacional: `10-guia-junior-piloto-deploy-proximos-passos.md`; frontend por feature: `14-arquitetura-frontend-por-feature.md`; continuidade/SLO: `15-operacao-continuidade-filas-s3-slo.md`; NFe sem chave: `16-nfe-xml-sem-chave-dedup.md`; versionamento API: `17-versionamento-api-rest.md`; decisões e aceite: `18-decisoes-e-checklist-entrega.md` |
| `deploy/prometheus/` | Exemplo de alertas: `alerts.lojapp.example.yml` |
| `scripts/` | Backup/restore Postgres (Docker): `backup-postgres-docker.ps1`, `restore-postgres-docker.ps1`; verificação Docker/WSL: `docker-wsl-check.sh` (bash) |
| `frontend/src/theme/tokens.css` | Tokens CSS globais (cores, raios, sombras, espaçamento); import antes de `App.css` |
| `frontend/src/features/` | Camadas `domain` / `application` / `presentation`; exemplos: `storefront`, `auth`, `orders`, `dashboard`, `sales`, `nfe`, `inventory`; template em `features/_template/README.md` |
| `.cursor/rules/` | Regras por contexto |
| `.cursor/plans/` | Planos aprovados |
