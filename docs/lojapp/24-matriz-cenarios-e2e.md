# Matriz de cenários E2E — cobertos x pendentes (P2.7)

Base: suíte Playwright em `frontend/e2e/session.spec.ts`.

## Cenários cobertos

| Categoria | Cenário | Estado |
|----------|---------|--------|
| Jornada principal | Acesso à landing pública (`/`) sem sessão | Coberto |
| Jornada principal | Acesso direto a `/login` sem sessão | Coberto |
| Jornada principal | Rota privada sem sessão redireciona para login | Coberto |
| Jornada principal | Login bem-sucedido abre painel (`/piloto/products`) | Coberto |
| Jornada principal | Logout volta para login e protege rota privada | Coberto |
| Falha crítica | Login com erro 500 mostra mensagem amigável | Coberto |
| Falha crítica | Login com erro 401 mantém utilizador no login | Coberto |
| Falha crítica | Login com erro 400 (`VALIDATION_ERROR`) mostra mensagem de validação | Coberto |
| Falha crítica | Falha de rede/timeout no login mostra mensagem de conectividade | Coberto |

## Cenários pendentes (prioridade)

| Categoria | Cenário pendente | Prioridade |
|----------|-------------------|------------|
| Sessão | Sessão expirada durante navegação numa rota privada (refresh 401 em runtime) | Alta |
| API painel | Erro 5xx em endpoint funcional do painel (ex.: produtos) com fallback visual adequado | Alta |
| Fluxo de auth | Registo com `CONFLICT` e `VALIDATION_ERROR` no ecrã de criação de conta | Média |
| UX de resiliência | Retentativa do utilizador após erro transitório sem recarregar página | Média |

## Observações de qualidade

- Seletores atuais usam preferencialmente `getByRole` e `getByLabel`, reduzindo fragilidade.
- Mensagens assertadas são orientadas ao utilizador (não a códigos técnicos), com regex para tolerar pequenas variações de texto.
