# Operação — continuidade, filas, S3, SLO e alertas (Sprint 6)

Documento de **decisão e execução** para fechar o backlog operacional da API LojApp: processos assíncronos, resiliência, armazenamento NFe em S3, backup PostgreSQL e primeira camada de SLO/alertas.

## 1. Filas e eventos (processos assíncronos)

**Estado actual:** a API é sobretudo **síncrona**; idempotência cobre vendas/ajustes críticos.

**Estratégia alvo (quando surgir carga ou integrações longas):**

| Cenário | Abordagem sugerida |
|--------|---------------------|
| Trabalho pesado desacoplado (ex.: relatórios, batch NFe) | Fila **persistente** (RabbitMQ ou SQS) + consumidor dedicado; mensagem com `userId`, tipo de trabalho e payload mínimo (ou referência a registo em DB). |
| Vários serviços a reagir ao mesmo facto | **Outbox** na mesma transacção + publicação assíncrona (padrão transactional outbox) para evitar duplicar eventos. |
| Apenas tolerar falhas em chamadas HTTP externas | **Resilience4j** (retry + circuit breaker) no cliente; não substitui fila para trabalho longo. |

**Política de retry e dead letter (DLQ):**

- **Produtor:** retry com backoff e jitter; máximo de tentativas definido por tipo de mensagem; após esgotar, mover para **DLQ** (fila morta ou tópico `_dlq`) com o mesmo schema + `lastError`, `failedAt`.
- **Consumidor:** operação idempotente sempre que possível (chave de negócio ou idempotency store); em falha não recuperável, **ack** para DLQ e alerta.
- **Observabilidade:** métrica `messages_consumed_total` / `messages_dlq_total` por tipo; trace ligado ao `traceparent` já suportado na API.

Nenhuma fila está **obrigatória** até existir um caso de uso que justifique (evitar infra sem necessidade).

## 2. S3 (NFe e XML)

Configuração da aplicação: `lojapp.nfe.storage` em `application.yml` (`database` ou `s3`, MinIO ou AWS).

**Checklist operacional (produção):**

1. **Bucket dedicado** (ex. `lojapp-nfe-xml`), região alinhada à API e ao utilizador.
2. **Versionamento** activado no bucket (recuperação de sobrescrita acidental).
3. **Cifrado em repouso** (SSE-S3 ou SSE-KMS conforme política da organização).
4. **IAM** com política mínima: `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` só no prefixo `lojapp.nfe.storage.s3.key-prefix`.
5. **Lifecycle:** transição para classe fria após N dias se o custo justificar; **retenção** alinhada a `raw-xml-retention-days` e obrigações fiscais (validar com contabilidade/auditoria — este doc não substitui aconselhamento legal).
6. **Backups:** replicação cross-region ou cópia periódica para outro bucket/conta se o RTO/RPO o exigirem.

## 3. Backup e restore PostgreSQL (testável)

### 3.1 Docker Compose (serviço `db`)

**Backup (formato custom `-Fc`, adequado a `pg_restore`):**

```powershell
# Na raiz do repositório (Windows PowerShell)
.\scripts\backup-postgres-docker.ps1
```

**Restore** (apaga dados actuais do volume se usar `--clean`; usar só em ambiente controlado):

```powershell
.\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\lojapp-YYYYMMDD-HHMMSS.dump
```

Equivalente manual (qualquer SO, após `docker compose up -d`):

```bash
docker compose exec -T db sh -c 'pg_dump -U lojapp -d lojapp -Fc -f /tmp/lojapp.dump'
docker compose cp db:/tmp/lojapp.dump ./backups/lojapp.dump
```

Restore manual:

```bash
docker compose cp ./backups/lojapp.dump db:/tmp/lojapp.dump
docker compose exec -T db sh -c 'pg_restore -U lojapp -d lojapp --clean --if-exists /tmp/lojapp.dump'
```

**Prova mínima (DoD):** executar backup → subir stack limpa (novo volume) ou outra instância → restore → smoke test (`/actuator/health`, login, listagem).

### 3.2 Produção

Usar snapshots geridos pelo fornecedor (RDS, Cloud SQL, etc.) **e** periodicamente validar restore para um ambiente isolado. Os scripts acima servem de modelo para VM + Docker; em PaaS, seguir o runbook do fornecedor.

## 4. SLO inicial e alertas (Prometheus)

Métricas expostas: `/actuator/prometheus` (com JWT ou `LOJAPP_ACTUATOR_METRICS_ANONYMOUS=true` só em dev).

**SLO de exemplo (produto interno):**

| Indicador | Objectivo inicial | Notas |
|-----------|-------------------|--------|
| Disponibilidade API | 99,5% / mês | `probe_success` externo ou synthetics; ou taxa 5xx &lt; 0,5% em `/api/**`. |
| Latência p99 `http.server.requests` | &lt; 2s em rotas críticas | Ajustar por rota quando houver cardinalidade controlada. |
| Erros 5xx | &lt; 1% durante 10 min | Ver regras de exemplo. |

Ficheiro de referência: `deploy/prometheus/alerts.lojapp.example.yml` — copiar para o teu Prometheus/Alloy e ajustar labels `job`, `application` e limiares.

**Alertmanager:** encaminhar `severity: critical` para canal on-call; `warning` para email/Slack.

## 5. Ligações com o resto do projecto

- Métricas HTTP e SLO em histograma: `management.metrics.distribution` em `application.yml`.
- Negócio: `lojapp.sales.registered`, `lojapp.nfe.imports`, `lojapp.idempotency.replay`.
- Tracing: `LOJAPP_TRACING_ENABLED`, export Zipkin opcional — ver comentários em `application.yml`.
- Logs JSON: perfil `jsonlogs` (compose dev/prod).

## 6. Checklist de aceite (entrega Sprint 6 — continuidade)

- [ ] Backup documentado + script executado com sucesso num ambiente de teste.
- [ ] Restore documentado + smoke test após restore.
- [ ] Regras de alerta importadas (ou equivalente) e um canal de notificação definido.
- [ ] Bucket S3 (se em uso) com versionamento e lifecycle revistos.
- [ ] Decisão registada sobre fila vs síncrono para o próximo trabalho assíncrono real.
