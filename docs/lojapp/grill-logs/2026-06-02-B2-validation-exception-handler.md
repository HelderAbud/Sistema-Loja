# Grill — B2 (HandlerMethodValidationException)

**Data:** 2026-06-02  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa B2

## Escopo

Query params inválidos em `DashboardController` (`@Min` em `brandOffset` / `brandLimit`) passam a responder **400** com `VALIDATION_ERROR`, em vez de **500** genérico.

**Nota de implementação:** em runtime (Spring Boot 3.5 + `@Validated` AOP) a exceção observada é `ConstraintViolationException`; mantido também handler para `HandlerMethodValidationException` (caminho dispatcher Spring 6.2+).

## Perguntas respondidas

| # | Pergunta | Resposta |
|---|----------|----------|
| 1 | `ApiErrorCode.VALIDATION_ERROR` existe? | Sim — já usado em `MethodArgumentNotValidException` |
| 2 | Outros controllers com `@Validated` em params? | Só `DashboardController` — handler central cobre este e futuros |
| 3 | ADR? | Não |

## Decisões

- [x] Handler em `GlobalExceptionHandler` + mensagem `param: detalhe` (estilo body validation)
- [x] Teste `DashboardControllerTest` com `brandOffset=-1` → 400 + código `VALIDATION_ERROR`
- [x] Branch `fix/B2-validation-exception-handler`

## Aprovado para executar?

- [x] Sim

## Próximo passo

**B3** — scripts backup/restore alinhados ao Compose
