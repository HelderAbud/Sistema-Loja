# LojApp — Plataforma de Gestão Comercial

[![CI](https://github.com/HelderAbud/Sistema-Loja/actions/workflows/ci.yml/badge.svg?branch=Principal)](https://github.com/HelderAbud/Sistema-Loja/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-WSL2%20%26%20Ubuntu-2496ED?logo=docker&logoColor=white)](docs/docker-wsl-ubuntu.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

SPA React + API Spring Boot para **gestão de loja física**: produtos, stock, vendas, importação de **NFe (XML)**, **PDV/caixa**, dashboard com KPIs, gráficos e **curva ABC**.

---

## Visão geral

| | |
|---|---|
| **Problema** | Sair da planilha frágil sem cair num ERP pesado |
| **Solução** | Fluxo MVP real — nota entra, stock atualiza, venda baixa saldo, indicadores apoiam compra e precificação |
| **Isolamento** | Dados isolados por conta (multi-loja: uma conta = uma loja) |
| **Diferenciais** | JWT + refresh com rotação, rate limit, Actuator/Prometheus, auditoria, OpenAPI, TanStack Query, skeletons no dashboard |

---

## Demo

| Serviço | Link |
|---------|------|
| Frontend (Vercel) | [sistema-loja-psi.vercel.app](https://sistema-loja-psi.vercel.app) |
| API (Render) | [lojapp-api.onrender.com](https://lojapp-api.onrender.com) |
| Health | [GET /actuator/health](https://lojapp-api.onrender.com/actuator/health) |
| Piloto (resultado) | [`docs/lojapp/piloto-demo-resultado.md`](docs/lojapp/piloto-demo-resultado.md) |

Swagger / OpenAPI **não** públicos em `prod` (HTTP 401). Conta demo sob HITL — registo público desligado. Free tier Render: o primeiro request após idle pode demorar (cold start).

---

## Screenshots

| Arquivo | Tela |
|---------|------|
| `01-login.png` | Login / registro |
| `02-dashboard.png` | Dashboard (KPIs + gráficos) |
| `03-vendas.png` | Histórico de vendas |
| `04-estoque.png` | Stock / inventário |
| `05-importacao-xml.png` | Importação NFe (XML) |
| `06-relatorios.png` | Relatórios (curva ABC / marcas) |

![Login](docs/screenshots/01-login.png)
![Dashboard](docs/screenshots/02-dashboard.png)
![Vendas](docs/screenshots/03-vendas.png)
![Estoque](docs/screenshots/04-estoque.png)
![Importação XML](docs/screenshots/05-importacao-xml.png)
![Relatórios](docs/screenshots/06-relatorios.png)

![Fluxo principal](docs/screenshots/07-fluxo-principal.gif)

Guia de captura: [`docs/screenshots/README.md`](docs/screenshots/README.md). Trilha portfólio: [`TRILHA-DIA-A-DIA.md`](TRILHA-DIA-A-DIA.md).

**Narrativa em 7 etapas:** [`docs/portfolio/etapas.md`](docs/portfolio/etapas.md)

### Pitch (60–90 s)

1. **Problema** — planilha frágil: stock errado, NFe manual, sem visão de margem.
2. **Solução** — LojApp: XML da NFe entra → stock atualiza → venda baixa saldo → dashboard (KPIs + curva ABC), dados isolados por loja.
3. **Stack** — Java 21, Spring Boot, PostgreSQL/Flyway, JWT + rate limit, React 19, CI no GitHub.
4. **Prova** — testes de concorrência de stock, isolamento multi-loja, Testcontainers + ArchUnit — não é só CRUD.
5. **Estado** — demo pública no ar (API Render + front Vercel); screenshots e pitch completo no repo.

Texto falado, 3 casos técnicos e roteiro de ensaio: [`docs/lojapp/pitch-portfolio.md`](docs/lojapp/pitch-portfolio.md).

---

## Stack

| Camada | Tecnologia |
|--------|------------|
| API | Java 21, Spring Boot 3.5, JPA, Flyway, PostgreSQL 16 |
| Segurança | JWT (access + refresh opaco com rotação), Bucket4j (rate limit), `@PreAuthorize` por role |
| Documentação | springdoc-openapi / Swagger (desativável em `prod`) |
| Observabilidade | Spring Actuator (`health`, `info`, `metrics`, `prometheus`), correlation id via `X-Request-Id` / MDC |
| Frontend | React 19, Vite 6, TypeScript, TanStack Query, Zustand, Recharts, Sonner |
| Testes | JUnit 5, Mockito, H2 (testes leves), Testcontainers + Postgres (integração) |

### Roles (`AppRole`)

| Role | Uso típico |
|------|------------|
| `USER` | Operador da loja (catálogo, vendas, NFe, dashboard) |
| `ADMIN` | Administração (`GET /users/admin/list`, etc.) |
| `REPRESENTATIVE` | Representante B2B (mesmo acesso operacional base que `USER`) |
| `CASHIER` / `SELLER` | PDV — finalização de venda |
| `MANAGER` | PDV — abertura/fecho de turno de caixa |

---

## Arquitetura

```mermaid
flowchart LR
  Browser[Browser] --> SPA[React SPA]
  SPA -->|JWT + refresh /api/v1| API[Spring Boot API]
  API --> PG[(PostgreSQL)]
  API --> Redis[(Redis)]
```

Visão em camadas (API):

```text
[ React SPA ]  ──── JWT + refresh ────▶  [ REST /api/v1 ]
                                               │
                   ┌───────────────────────────┼──────────────────────┐
                   │                           │                      │
             AuthController           LojApp controllers        Actuator /prometheus
                   │                           │                      │
            Auth*UseCase                 application/              Métricas JVM/HTTP
                   │                    (use cases)                      │
              AuthService                  Services                      │
                   │                           │                      │
             refresh_tokens              Repositories
             audit_logs                  PostgreSQL (Flyway V1…V20)
```

**Camadas:** controllers finos → `application` (use cases) + `service` → `repository`. DTOs para contratos HTTP; entidades em `entity`. ArchUnit valida dependências entre camadas.

**Auditoria:** tabela `audit_logs` com eventos (`AUTH_LOGIN`, `SALE_CREATED`, `NFE_IMPORT`, `STOCK_ADJUST`, …).

**Dashboard:** `/dashboard/inventory-kpis`, `/dashboard/brands`, `/dashboard/products-abc` (curva ABC).

**PDV:** turnos de caixa (`/pos/cash-sessions/*`) e finalização de venda (`/pos/sales/finalize`).

### Formato de erro da API

Respostas de erro seguem `ApiErrorResponse` (tratado em `GlobalExceptionHandler`):

```json
{
  "message": "Texto legível para o utilizador ou integrador.",
  "code": "VALIDATION_ERROR",
  "status": 400,
  "path": "/api/v1/lojapp/products",
  "timestamp": "2026-04-24T12:00:00Z"
}
```

O campo `code` usa valores do enum `ApiErrorCode` (`BAD_REQUEST`, `FORBIDDEN`, `CONFLICT`, `INTERNAL_ERROR`, …). O SPA consome este formato em `frontend/src/api.ts`.

---

## Fluxo principal

1. **Auth** — registro/login devolve `accessToken` (memória do browser); refresh opaco em cookie HttpOnly (`lojapp_rt`, path `/api/v1/auth`). Ao abrir a app, renova o access a partir dessa cookie.
2. **Catálogo** — marcas, fornecedores, coleções, modelos e produtos; ajuste de stock ou entrada via importação NFe.
3. **Vendas** — registam movimento `SALE` e reduzem saldo; PDV com pagamentos e turno de caixa.
4. **Dashboard** — faturamento/lucro por marca, top produtos, curva ABC (80/15/5 %) e alertas de stock baixo.

### Importação NFe → stock

```mermaid
sequenceDiagram
  participant UI as SPA
  participant API as LojApp API
  participant DB as PostgreSQL
  UI->>API: POST /nfe/import (rawXml)
  API->>API: parse XML seguro + match produto
  API->>DB: nfe_entries, nfe_items, movimento ENTRY
  API->>DB: atualiza inventory_balances
  API-->>UI: nfeEntryId, nfeNumber, importedItems
```

---

## Portas (referência única)

| Serviço | Porta host | Onde está definido |
|---------|------------|-------------------|
| **API Spring Boot** | **8081** | `application.yml`, `docker-compose.yml`, `docker-compose.prod.yml`, `vite.config.ts` (proxy) |
| **Frontend Vite (dev)** | **5173** | `frontend/vite.config.ts` |
| **PostgreSQL** | **5433** (host → 5432 no contentor) | `docker-compose.yml` |
| **Redis** | **6381** (host → 6379 no contentor) | `docker-compose.yml` |

**Links locais (dev):**

| | URL |
|---|-----|
| Frontend | http://localhost:5173 |
| Login | http://localhost:5173/login |
| Swagger | http://localhost:8081/swagger-ui.html |
| Health | http://localhost:8081/actuator/health |

O proxy Vite encaminha `/api/*` → `http://localhost:8081`. Se a API não estiver na **8081**, o front devolve 502.

---

## Como rodar (local)

**Quickstart (5 linhas):**

```bash
cp .env.example .env          # POSTGRES_PASSWORD + LOJAPP_JWT_SECRET (≥32 chars)
docker compose up -d db redis
./mvnw spring-boot:run        # API → http://localhost:8081
cd frontend && npm install && npm run dev
# SPA → http://localhost:5173 · health → http://localhost:8081/actuator/health
```

**Requisitos:** Java 21, Maven 3.9+, Node 20+, Docker (recomendado para Postgres/Redis).  
Maven Wrapper: `./mvnw` (Linux/Mac) ou `.\mvnw.cmd` (Windows). Detalhe dos fluxos e variáveis abaixo.

### 0. Variáveis de ambiente (obrigatório)

```bash
cp .env.example .env
```

Preencha no `.env`:

| Variável | Obrigatório | Descrição |
|----------|-------------|-----------|
| `POSTGRES_PASSWORD` | Sim (Docker) | Password do Postgres no Compose |
| `LOJAPP_JWT_SECRET` | Sim | Segredo JWT (≥ 32 caracteres) |
| `SPRING_DATASOURCE_PASSWORD` | Sim (Maven + Docker) | Mesmo valor que `POSTGRES_PASSWORD` |
| `SPRING_DATASOURCE_URL` | Recomendado | Com Postgres do Compose: `jdbc:postgresql://localhost:5433/loja_db` |
| `SPRING_DATASOURCE_USERNAME` | Recomendado | `loja_user` (Compose dev) |
| `LOJAPP_CORS_ORIGINS` | Produção | Origens do frontend |
| `VITE_API_BASE` | Build prod | URL pública da API (sem barra final) |

> **Não commite `.env`.** O repositório inclui apenas `.env.example`.

> **Segredos por ambiente.** Gere um `LOJAPP_JWT_SECRET` **próprio** em cada máquina/deploy
> (≥ 32 caracteres). Não reutilize segredos de exemplo nem de outros ambientes — um valor de
> demonstração já circulou no histórico Git e não deve ser usado.
>
> ```bash
> # Linux/macOS
> openssl rand -base64 48
> ```
> ```powershell
> # Windows PowerShell
> -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | % {[char]$_})
> ```
>
> Após rotacionar o segredo, os tokens emitidos com o valor anterior deixam de validar — faça login novamente.

### Fluxo A — recomendado para desenvolvimento diário

Infra no Docker; API com Maven (hot reload, alinhado ao proxy Vite):

```bash
docker compose up -d db redis
./mvnw spring-boot:run          # API → http://localhost:8081
cd frontend && npm install && npm run dev   # SPA → http://localhost:5173
```

### Fluxo B — stack completa no Docker

```bash
docker compose up -d
# API → http://localhost:8081 (mesma porta que Maven e proxy Vite)
cd frontend && npm install && npm run dev
```

> **Não corra `mvn spring-boot:run` e o serviço `api` do Compose em simultâneo** — ambos usam a porta **8081** e o mesmo Postgres/Redis.

### Fluxo C — produção local (exemplo)

```bash
docker compose -f docker-compose.prod.yml up -d
# API prod → http://localhost:8081 (Swagger desligado)
```

### Backup e restore (Postgres no Docker)

Scripts em `scripts/` alinham credenciais ao ficheiro Compose:

| Compose | Utilizador | Base de dados |
|---------|------------|---------------|
| `docker-compose.yml` (dev) | `loja_user` | `loja_db` |
| `docker-compose.prod.yml` | `lojapp` | `lojapp` |

```powershell
# Dev (stack com db a correr)
.\scripts\backup-postgres-docker.ps1 -ComposeFile docker-compose.yml
.\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\loja_db-<timestamp>.dump -ComposeFile docker-compose.yml

# Prod-like
.\scripts\backup-postgres-docker.ps1 -ComposeFile docker-compose.prod.yml
```

Os ficheiros `.dump` ficam em `backups/` (não versionados). O restore usa `--clean` — só em ambiente descartável.

### Docker no WSL2 / Ubuntu

Se aparecer `permission denied` ao conectar ao Docker daemon, siga [docs/docker-wsl-ubuntu.md](docs/docker-wsl-ubuntu.md).

### Executar testes

```bash
# Backend
./mvnw test

# Frontend
cd frontend
npm run lint
npm run test
npm run e2e
```

### O que o CI prova (honestidade)

| Job | O que cobre | Limite |
|-----|-------------|--------|
| `backend-unit` (`-Pci-unit-tests`) | JUnit/Mockito + ArchUnit; **exclui** `*IntegrationTest` | Não sobe Postgres |
| `backend-integration` | `*IntegrationTest` com Testcontainers/Postgres | Requer Docker no runner |
| `frontend` → Vitest | Componentes/hooks unitários | Sem API real |
| `frontend` → Playwright (`npm run e2e`) | `session.spec.ts` e afins com **API auth mockada** (`page.route`) | **Não** é fluxo E2E contra API/DB reais |
| Fluxo real | `e2e/real-flow.spec.ts` | **Ignorado** no Playwright default (`testIgnore`); opt-in local |

Workflow canónico: [`.github/workflows/ci.yml`](.github/workflows/ci.yml). O ficheiro legado `backend-ci.yml` está desativado.
---

## Troubleshooting

### `POST /api/v1/auth/login` retorna 401

O login usa `AuthLoginUseCase` → `UserRepository.findByEmailIgnoreCase` + `PasswordEncoder.matches` em `password_hash`. 401 quase sempre = email inexistente ou senha errada.

**1. Verificar utilizador no Postgres** (Compose dev: `loja_user` / `loja_db`, contentor `loja-postgres`):

```bash
docker exec -it loja-postgres psql -U loja_user -d loja_db -c "
SELECT id, email, left(password_hash, 7) AS prefix, length(password_hash) AS tamanho
FROM users
WHERE lower(email) = lower('exemplo@email.com');
"
```

**2. Interpretar o resultado:**

| Resultado | Causa provável |
|-----------|----------------|
| 0 linhas | Utilizador inexistente — use `POST /api/v1/auth/register` (`LOJAPP_REGISTRATION_ENABLED=true` no Compose dev) |
| `prefix` `$2a$`/`$2b$`/`$2y$`, `tamanho = 60` | Hash BCrypt plausível — confira senha e email |
| Texto plano ou `tamanho ≠ 60` | Atualize com BCrypt (12 rounds) |

**3. Gerar hash BCrypt:**

```bash
docker run --rm python:3.12-alpine sh -c \
  "pip install -q bcrypt && python -c \"import bcrypt; print(bcrypt.hashpw(b'SUA_SENHA', bcrypt.gensalt(rounds=12)).decode())\""
```

**4. Porta ao testar com `curl`:** API em **http://localhost:8081** (`http://localhost:8081/api/v1/auth/login`).

**5. `curl: (56) Connection reset by peer`:** veja `docker logs loja-api --tail 80`. Causas: JDBC com host errado (use **`db`** na rede Compose), Postgres a iniciar, `LOJAPP_JWT_SECRET` ausente, erro Flyway.

### Frontend 502 em `/api`

A API não está em **http://localhost:8081**. Confirme com `curl http://localhost:8081/actuator/health`.

---

## API — Rotas principais (`/api/v1`)

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/auth/register` | Registro → tokens |
| `POST` | `/auth/login` | Login → tokens |
| `POST` | `/auth/refresh` | Renovar par access + refresh |
| `POST` | `/auth/logout` | Terminar sessão (limpa cookie) |
| `GET` | `/users/me` | Utilizador autenticado |
| `GET` | `/lojapp/dashboard/brands` | KPI por marca (`from`, `to`) |
| `GET` | `/lojapp/dashboard/products-abc` | Curva ABC / top produtos |
| `GET` | `/lojapp/dashboard/inventory-kpis` | Totais SKUs, unidades, stock baixo |
| `GET/POST` | `/lojapp/products`, `/brands`, `/suppliers`, … | Catálogo e hierarquia |
| `POST` | `/lojapp/sales` | Registar venda |
| `POST` | `/lojapp/sales/{id}/cancel` | Cancelar venda |
| `POST` | `/lojapp/nfe/import` | Importar XML NFe |
| `POST` | `/lojapp/inventory/adjust` | Ajuste manual de stock |
| `POST` | `/lojapp/pos/sales/finalize` | Finalizar venda PDV |
| `POST/GET` | `/lojapp/pos/cash-sessions/*` | Turno de caixa (abrir/fechar) |

Documentação completa: **http://localhost:8081/swagger-ui.html** (desligado em `prod`).

**Actuator:** em dev expõe `metrics` e `prometheus`. Em `SPRING_PROFILES_ACTIVE=prod` apenas `health` e `info` por defeito. Override: `LOJAPP_MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`.

---

## Scripts operacionais

| Caminho | Objetivo |
|---------|----------|
| `scripts/verify-api-env.ps1` / `.sh` | Verifica variáveis antes de subir/deploy |
| `scripts/import-nfe-folder.sh` | Importa lote de XMLs NFe |
| `scripts/run-nfe-integration-tests.sh` | Bateria de testes integração NFe |
| `scripts/git-untrack-frontend-artifacts.ps1` | Remove artefatos do índice Git |
| `scripts/package-source-safe.ps1` | ZIP seguro do código-fonte |
| `scripts/verify-zip-safe.ps1` | Valida ZIP (sem `.env`, `target/`, etc.) |

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-source-safe.ps1 -OutputZip "C:\temp\lojapp-safe.zip"
powershell -ExecutionPolicy Bypass -File scripts/verify-zip-safe.ps1 -ZipPath "C:\temp\lojapp-safe.zip"
```

---

## Git: não versionar artefatos de build

O `.gitignore` exclui `node_modules`, `dist`, `target` e `build`. Se entraram no índice:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/git-untrack-frontend-artifacts.ps1
```

---

## Deploy

- **Backend:** imagem Docker (JAR + perfil `prod`), Postgres gerenciado. Defina `SPRING_PROFILES_ACTIVE=prod`, `LOJAPP_JWT_SECRET` forte, `LOJAPP_CORS_ORIGINS`.
- **Frontend:** `npm run build` em Vercel, Netlify ou CDN; `VITE_API_BASE` apontando para a API.
- **All-in-one:** Railway, Render, Fly.io ou VPS — ver `docker-compose.prod.yml`.

Guia detalhado: [`docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md`](docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md).

## Smoke demo (opcional)

Links públicos: secção [Demo](#demo) no topo.

```powershell
$env:API_BASE = 'https://lojapp-api.onrender.com'
$env:LOJAPP_VERIFY_EMAIL = 'sua-conta-demo@exemplo.com'
$env:LOJAPP_VERIFY_PASSWORD = '...'
.\scripts\verify-api-env.ps1
```

---

## Resultados do MVP

- Fluxo ponta a ponta: registro/login, catálogo, stock, venda, NFe, dashboard e PDV.
- Segurança: JWT com refresh, `@PreAuthorize`, auditoria e isolamento por `user_id`.
- Qualidade: testes unitários, ArchUnit, Testcontainers, CI (GitHub Actions).
- Schema versionado com Flyway (V1…V20).

## Próximos passos

- PWA / modo offline leve; code-split do bundle do dashboard.
- Expandir `@PreAuthorize` e políticas por role (admin multi-loja).
- Soft delete em produtos; cache (Caffeine) em leituras frequentes.
- Export CSV/PDF do dashboard; integração fiscal adicional.

---

## Documentação

- [CONTRIBUTING.md](CONTRIBUTING.md) — branches, PR, autor único
- [CHECKLIST_FINAL.md](CHECKLIST_FINAL.md) — auditoria pré-portfólio
- [Escopo MVP](docs/lojapp/01-escopo-mvp.md)
- [Resultado do piloto demo (Fase C)](docs/lojapp/piloto-demo-resultado.md)
- [Plano piloto / implantação](docs/lojapp/03-implantacao-pilotos.md)
- [Guia deploy e próximos passos](docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md)
- [Docker + WSL2 / Ubuntu](docs/docker-wsl-ubuntu.md)
- [Trilha dia a dia](TRILHA-DIA-A-DIA.md)

## Licença

Distribuído sob a licença [MIT](LICENSE). Copyright (c) 2026 Helder Abud.

---

Repositório: [HelderAbud/Sistema-Loja](https://github.com/HelderAbud/Sistema-Loja)
