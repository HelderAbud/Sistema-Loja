# Validação de observabilidade e rastreabilidade (P1.5)

Objetivo: registrar evidências técnicas da validação de logs correlacionados, métricas padronizadas, tracing HTTP e alertas de referência.

## 1) Logs estruturados com correlação

- `application.yml` inclui no padrão de log: `rid` (`requestId`), `uid` (`userId`), `trace` (`traceId`) e `span` (`spanId`).
- `RequestCorrelationFilter` injeta/propaga `X-Request-Id` e coloca o valor no MDC.
- `ApiAccessLogFilter` registra `http_access method path status durationMs` e adiciona `userId` no MDC quando autenticado.
- Evidência automatizada: `RequestCorrelationIntegrationTest` valida eco do `X-Request-Id` e geração automática quando ausente.

## 2) Métricas técnicas e de negócio

- Métricas de negócio centralizadas em `LojappBusinessMetrics`:
  - `lojapp.sales.registered`
  - `lojapp.nfe.imports` (`outcome=success|duplicate_key|duplicate_xml`)
  - `lojapp.idempotency.replay` (`scope=...`)
- Métricas de sessão em `AuthSessionMetrics`:
  - `lojapp.auth.refresh` (`outcome=success|expired|invalid|unexpected`)
- Métricas HTTP padrão Micrometer ativadas com histograma/SLO para `http.server.requests` em `application.yml`.

## 3) Tracing HTTP (propagação e visibilidade)

- Tracing configurável por ambiente:
  - `LOJAPP_TRACING_ENABLED`
  - `LOJAPP_TRACING_SAMPLE_PROBABILITY`
  - export opcional via Zipkin (`LOJAPP_ZIPKIN_EXPORT_ENABLED`, `LOJAPP_ZIPKIN_ENDPOINT`)
- Visibilidade em logs garantida pelos campos `traceId` e `spanId` no padrão de logging.

## 4) Alertas Prometheus (revisão de cobertura)

Arquivo base revisado: `deploy/prometheus/alerts.lojapp.example.yml`

- Coberturas existentes: taxa de 5xx, burst 5xx, p99 de latência, pico de NFe duplicada.
- Lacunas tratadas nesta fase:
  - alerta de indisponibilidade de instância (`LojappApiInstanceDown`)
  - alerta de falhas inesperadas no refresh (`LojappAuthRefreshUnexpectedSpike`)

## 5) Pendências conhecidas

- Ajustar label `job` do alerta `LojappApiInstanceDown` ao scrape real de cada ambiente.
- Validar firing real dos alertas em ambiente com Prometheus/Alertmanager ativos.
