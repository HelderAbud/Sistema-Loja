# Go/No-Go de deploy (produção)

Checklist único e auditável para decidir se a release pode ir para produção.

## Pré-condições obrigatórias

- [ ] `POSTGRES_PASSWORD` definido no ambiente de deploy.
- [ ] `LOJAPP_JWT_SECRET` definido com valor forte (>= 32 bytes).
- [ ] `LOJAPP_CORS_ORIGINS` alinhado aos domínios reais do frontend.
- [ ] `LOJAPP_TRUST_FORWARD_HEADERS` configurado conforme presença de proxy confiável.

## Artefato e container

- [ ] Build da API concluído sem erro: `mvn -q -DskipTests package`.
- [ ] Imagem API usa utilizador não-root (`USER lojapp`) no `Dockerfile`.
- [ ] JVM com flags para container (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`).
- [ ] `docker compose -f docker-compose.prod.yml config` sem erros de sintaxe/expansão.

## Compose de produção

- [ ] `db` com healthcheck `pg_isready`.
- [ ] `redis` com healthcheck `redis-cli ping`.
- [ ] `api` com healthcheck de readiness: `/actuator/health/readiness`.
- [ ] `depends_on` condicionado a serviços healthy (db/redis).
- [ ] Limites de recursos definidos para `api` (`cpus` e `memory`).

## Readiness/Liveness operacional

- [ ] Após subir stack, `GET /actuator/health` responde `UP`.
- [ ] `GET /actuator/health/readiness` responde `UP` (sem JWT — necessário para healthcheck Docker/K8s).
- [ ] `GET /actuator/health/liveness` responde `UP`.

Exemplo de validação local:

```powershell
docker compose -f docker-compose.prod.yml up -d
curl -sf http://localhost:8080/actuator/health | jq .
curl -sf http://localhost:8080/actuator/health/readiness | jq .
curl -sf http://localhost:8080/actuator/health/liveness | jq .
```

Ou, sem `jq`: `.\scripts\verify-deploy-health.ps1` (ver também `32-checklist-hardening-deploy-dia9.md`).

## Critério final

- [ ] **Go**: todos os itens acima concluídos com evidência (log, saída de comando, screenshot ou link de pipeline).
- [ ] **No-Go**: qualquer item obrigatório pendente bloqueia deploy.
