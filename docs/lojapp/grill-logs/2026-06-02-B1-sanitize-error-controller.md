# Grill — B1 (sanitizar LojappErrorController)

**Data:** 2026-06-02  
**Participantes:** utilizador + agente  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa B1

## Escopo

Corrigir vazamento de detalhes internos no fallback `/error` quando a exceção não passa pelo `GlobalExceptionHandler` (filtros, Tomcat, infra).

## Perguntas respondidas

| # | Pergunta | Resposta acordada | Recomendação |
|---|----------|-------------------|--------------|
| 1 | Modelo de teste? | `GlobalExceptionHandlerTest` (unitário com mocks de `HttpServletRequest`) | Assert no corpo JSON, não no log |
| 2 | Frontend depende de texto específico neste path? | Não — `ApiErrorBody` genérico em `frontend/src/api/client` | Mensagens PT alinhadas ao handler global |
| 3 | ADR necessário? | **Não** — restaura policy já definida em `server.error.include-message: never` | — |

## Comportamento acordado

| HTTP | Mensagem ao cliente | Log servidor |
|------|---------------------|--------------|
| 5xx | `Erro interno do servidor` (igual `GlobalExceptionHandler`) | ERROR com stack completo |
| 4xx | Whitelist por status (404, 403, …) | WARN se houver exceção |
| — | Nunca `ex.getMessage()` nem `ERROR_MESSAGE` no JSON | — |

## Decisões

- [x] `buildSafeClientMessage(HttpStatus)` testável (package-visible static)  
- [x] Teste de regressão com `relation "foo" does not exist`  
- [x] Branch `fix/B1-sanitize-error-controller`

## Aprovado para executar?

- [x] Sim

## Próximo passo no plano

**B2** — `HandlerMethodValidationException` no `GlobalExceptionHandler`
