# Template de feature (Sprint 5)

Estrutura alvo por capacidade de produto:

```
features/<nome>/
  domain/        # tipos puros, validações, funções sem React nem HTTP
  application/   # hooks, orquestração, estado (Zustand), chamadas à API via módulos em src/api
  presentation/  # componentes específicos da feature (opcional se a UI vive em pages/)
  index.ts       # reexport público estável da feature
```

## Migração incremental

1. Extrair primeiro **domain** (fácil de testar com Vitest).
2. Mover hooks / estado para **application**.
3. Deixar rotas em `pages/` a importar de `features/<nome>` até ser possível colocar páginas em `presentation/`.

## Imports

- `domain` não importa `application` nem `presentation`.
- `application` pode importar `domain` e `src/api`, `src/authStore`, etc.
- `presentation` importa `application` e `domain` conforme necessário.
