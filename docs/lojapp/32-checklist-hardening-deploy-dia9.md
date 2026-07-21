# Checklist — Hardening de deploy (Dia 9)

Objetivo: validar **candidato de deploy** (compose/imagem, perfis, origens CORS, probes) antes da demo pública (Dia 10).  
Complementa [21-go-no-go-deploy-producao.md](./21-go-no-go-deploy-producao.md) com foco no **ambiente candidato** e comandos reprodutíveis.

## 1. Configuração de deploy alvo

| Item | Onde / variável | Ação |
|------|-----------------|------|
| Domínio da API | DNS / reverse proxy → `api` | URL estável documentada; TLS terminado no proxy ou na app |
| Domínio do frontend | `LOJAPP_CORS_ORIGINS` (CSV) | **Todas** as origens HTTPS reais do SPA; sem wildcard em produção |
| Perfis Spring | `SPRING_PROFILES_ACTIVE` | `docker-compose.prod.yml`: `prod,jsonlogs` — Swagger desligado em `application-prod.yml` |
| Trust em forwarded headers | `LOJAPP_TRUST_FORWARD_HEADERS` | `true` só atrás de LB/nginx que sanitiza `X-Forwarded-*` |
| Segredos | `POSTGRES_PASSWORD`, `LOJAPP_JWT_SECRET`, etc. | Igual ao [21-go-no-go](./21-go-no-go-deploy-producao.md) |
| Cookie refresh HTTPS | `LOJAPP_AUTH_REFRESH_COOKIE_SECURE` | `true` em deploy com TLS (default no compose prod) |

## 2. Artefato (imagem e compose)

Referência técnica consolidada em **21** — resumo:

- `Dockerfile`: utilizador não-root (`USER lojapp`), `curl` na imagem para healthcheck, JVM `-XX:+UseContainerSupport` e `MaxRAMPercentage`.
- `docker compose -f docker-compose.prod.yml`: `db`/`redis` healthy antes de `api`; healthcheck da API em `/actuator/health/readiness`.

Validação **só de sintaxe** do compose (sem subir stack; valores dummy):

```powershell
.\scripts\verify-compose-prod-config.ps1
```

## 3. Health, readiness e liveness

Com `management.endpoint.health.probes.enabled: true` (ver `application.yml`), expor:

| URL | Função |
|-----|--------|
| `GET /actuator/health` | Estado agregado |
| `GET /actuator/health/readiness` | Pronto para receber tráfego (BD, etc.) |
| `GET /actuator/health/liveness` | Processo vivo |

**Nota (perfil `prod`):** o Actuator expõe por defeito apenas `health` e `info` (`application-prod.yml`); os **subcaminhos** `/readiness` e `/liveness` continuam acessíveis quando os probes estão ativos.

Verificação automática (API já a correr):

```powershell
.\scripts\verify-deploy-health.ps1
.\scripts\verify-deploy-health.ps1 -ApiBase "https://api.seu-dominio.com"
```

Linux / macOS:

```bash
./scripts/verify-deploy-health.sh
API_BASE=https://api.seu-dominio.com ./scripts/verify-deploy-health.sh
```

## 4. Checklist go-live no ambiente candidato

Executar no **mesmo** ambiente que será usado na demo (ou réplica 1:1):

- [ ] Stack sobe sem erro: `docker compose -f docker-compose.prod.yml up -d` (ou pipeline equivalente).
- [ ] `verify-compose-prod-config.ps1` passou em CI ou antes do push.
- [ ] `verify-deploy-health.ps1` com `-ApiBase` apontando para o candidato → três checks **UP**.
- [ ] `verify-api-env.ps1` (com utilizador de teste) ou smoke manual: login → endpoint protegido.
- [ ] Frontend aponta para a API certa (`VITE_API_BASE` / build de produção).
- [ ] Decisão **Go/No-Go** registada em [21-go-no-go-deploy-producao.md](./21-go-no-go-deploy-producao.md) (secção final).

## 5. Registo da execução (Dia 9)

| Data | Ambiente | health | readiness | liveness | Notas |
|------|----------|--------|-----------|----------|-------|
| 2026-07-21 | Render `lojapp-api` | UP (200) | 401 (auth) | 401 (auth) | Swagger/OpenAPI 401; registo público 403; JWT via smoke Dia 8; ver `grill-logs/validation-2026-07-21-trilha-dia-9.md` |
