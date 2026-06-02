# Performance com evidências (P2.9)

Escopo desta iteração:
- Endpoints/funções críticas relacionadas a `sales`, `dashboard` e `nfe/import`.
- Medição via teste de integração `PerformanceP2SmokeIntegrationTest` (ambiente local de teste).

## Método de medição

- Comando baseline: `mvn -B "-Dtest=PerformanceP2SmokeIntegrationTest" test`
- Comando após otimização: `mvn -B "-Dtest=PerformanceP2SmokeIntegrationTest,SalesServiceTest,DashboardLoadIntegrationTest" test`
- Métricas reportadas por execução: `p95`, `p99`, `avg` em ms.

## Resultado antes/depois

| Fluxo | Baseline (p95/p99/avg) | Após otimização (p95/p99/avg) | Variação observada |
|------|--------------------------|--------------------------------|--------------------|
| `sales.summary` | `1 / 71 / 2 ms` | `0 / 69 / 1 ms` | melhoria leve |
| `dashboard.brands` | `0 / 42 / 1 ms` | `0 / 27 / 0 ms` | melhoria relevante em p99 |
| `nfe.import` | `17 / 56 / 14 ms` | `23 / 56 / 16 ms` | sem ganho (variação local) |

## Otimização aplicada (baixo risco)

- Cache Caffeine para agregados de vendas:
  - `salesSummary`
  - `salesDaily`
- Evicção de caches de dashboard + vendas em mutações de venda:
  - `registerSale`
  - `cancelSale`
- Arquivos alterados:
  - `src/main/java/com/lojapp/config/CacheNames.java`
  - `src/main/java/com/lojapp/config/CacheConfig.java`
  - `src/main/java/com/lojapp/service/SalesService.java`

## Observações

- `DashboardLoadIntegrationTest` ficou `skipped` no ambiente atual por depender de Docker/Testcontainers.
- A medição atual é útil para comparação relativa de código na mesma máquina; para decisão de capacidade de produção, executar carga em ambiente mais próximo de produção (PostgreSQL real + dados representativos).
