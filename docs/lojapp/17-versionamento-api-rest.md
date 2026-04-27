# Versionamento da API REST (`/api/v1`)

## Prefixo atual

Todos os endpoints públicos da aplicação usam o prefixo **`/api/v1`**. Exemplos: `/api/v1/auth/login`, `/api/v1/lojapp/products`.

## Mudanças compatíveis (mesma versão)

- Novos campos **opcionais** em JSON de resposta.
- Novos endpoints sob o mesmo prefixo.
- Novos valores em enums documentados no OpenAPI, desde que clientes ignorem valores desconhecidos.

## Mudanças incompatíveis (breaking)

Exigem **nova versão** de URL (ex. `/api/v2/...`) ou negociação explícita acordada:

- Remover ou renomear campos obrigatórios.
- Alterar semântica de códigos HTTP ou códigos de erro estáveis.
- Remover endpoints ou alterar contratos de autenticação.

## Processo

1. Documentar a alteração no PR e, se necessário, neste diretório `docs/lojapp/`.
2. Preferir deprecação (header, campo `deprecated` no OpenAPI) antes de remover.
3. Manter `/api/v1` estável o máximo possível para SPAs e integrações em piloto.
