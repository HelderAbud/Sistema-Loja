# Contratos de autenticação e sessão (LojApp)

Documento para **onboarding técnico**: entradas, saídas, expiração, cookies e erros, alinhado ao código em `com.lojapp` (Spring Security + JWT + refresh opaco em base de dados). Para ameaças e mitigações ver também [13-threat-model-auth-spa.md](./13-threat-model-auth-spa.md).

## Modelo de sessão

| Peça | Formato | Onde vive |
|------|---------|-----------|
| **Access token** | JWT assinado (HS256) | Resposta JSON `accessToken`; cliente envia cabeçalho `Authorization: Bearer` + JWT |
| **Refresh token** | String opaca (dois UUID sem hífen); só o **hash SHA-256** é persistido | Cookie HTTP-only **ou** corpo JSON (compat); armazenamento em tabela `refresh_token` |

Cada **login**, **registo** bem-sucedido ou **refresh** válido emite **novo par** access + refresh. O refresh anterior é **consumido** (apagado) na rotação — reutilizar o mesmo refresh devolve erro (proteção a replay). Métrica Prometheus: `lojapp.auth.refresh` com tag `outcome` = `success` | `expired` | `invalid`.

## Tempos de vida (configuração)

Propriedades `lojapp.jwt` em `application.yml` (sobrescrever por variáveis de ambiente):

| | Propriedade / env | Default (exemplo) |
|--|-------------------|-------------------|
| Access | `expiration-ms` / `LOJAPP_JWT_EXPIRATION_MS` | 900000 ms (~15 min) |
| Refresh | `refresh-expiration-ms` / `LOJAPP_JWT_REFRESH_EXPIRATION_MS` | 1209600000 ms (~14 dias) |

Segredo: `LOJAPP_JWT_SECRET` — mínimo **32 bytes** (validado ao arranque).

## JWT de acesso (claims)

Gerado em `JwtService.createToken`:

- `sub`: id do utilizador (string numérica)
- `email`: email
- `role`: papel normalizado (`USER`, `ADMIN`, `REPRESENTATIVE`, …)

### Papel `REPRESENTATIVE` (B2B)

Utilizadores com `app_role` representante (ou alias aceite em `AppRole`) obtêm as mesmas capacidades operacionais **LojApp** que `USER` nos endpoints sob `@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")` — dados continuam isolados por `user_id`. Não existe portal separado; o painel piloto pode mostrar o papel via `GET /api/v1/users/me` (`appRole` no JSON). Atribuição do papel: atualizar coluna `app_role` na tabela de utilizadores (ex.: migração ou consola SQL em ambientes controlados).
- `iat` / `exp`: emissão e expiração conforme `expiration-ms`

## Cookie do refresh

Configuração `lojapp.auth`:

| Atributo | Default | Notas |
|----------|---------|--------|
| Nome | `lojapp_rt` | `lojapp.auth.refresh-cookie-name` |
| Path | `/api/v1/auth` | Cookie **não** é enviada nos restantes endpoints da API |
| HttpOnly | `true` | |
| SameSite | `Lax` | |
| Secure | `false` em dev | Em produção com HTTPS: `lojapp.auth.refresh-cookie-secure=true` |
| Max-Age | derivado do TTL do refresh | segundos = `refreshExpirationMs / 1000` (mínimo 1) |

`Set-Cookie` é emitido em **register**, **login**, **refresh** (nova rotação). **logout** devolve cookie com valor vazio e `Max-Age=0`.

## Endpoints (`/api/v1/auth`)

Todos são **POST** e públicos na cadeia Spring (`permitAll` para `POST /api/v1/auth/**`).

### `POST /api/v1/auth/register`

- **Body:** `{ "email": string, "password": string, "inviteToken"?: string }` — password 8–128 caracteres; email validado. Se `lojapp.auth.registration.invite-secret` estiver definido no ambiente, `inviteToken` é **obrigatório** e deve coincidir (partilhar só por canal seguro; ver `LOJAPP_REGISTRATION_INVITE_SECRET`).
- **200:** `{ "accessToken": "<jwt>" }` + `Set-Cookie` refresh.
- **400:** validação / JSON inválido — corpo `ApiErrorResponse`.
- **403:** registo desativado, domínio não permitido ou convite inválido — `ApiErrorResponse`.
- **409:** conflito de dados — `ApiErrorResponse` (registo: tipicamente email já existente; outras rotas: unicidade ou integridade referencial, sem detalhe SQL).
- **429:** limite de registos por IP/hora — **texto plain** + `Retry-After: 3600` (ver rate limit).

### `POST /api/v1/auth/login`

- **Body:** `{ "email": string, "password": string }`.
- **200:** `{ "accessToken": "<jwt>" }` + `Set-Cookie` refresh.
- **400:** validação — `ApiErrorResponse`.
- **401:** credenciais inválidas — `ApiErrorResponse`.
- **429:** login/refresh por IP — texto plain + `Retry-After: 60`.

### `POST /api/v1/auth/refresh`

- **Body:** opcional `{ "refreshToken": string }` (para clientes que não usam cookie).
- **Fonte do refresh:** se existir cookie **e** corpo com token, os valores têm de ser **iguais**; caso contrário **401** com mensagem `Refresh token inconsistente`. Se ambos presentes e iguais, prevalece o valor (equivalente). Na prática: tipicamente só cookie **ou** só body.
- Se cookie existir e body não estiver vazio: regra acima aplica-se.
- Ordem efectiva: se cookie não vazia → usa-se cookie; senão → body.
- **200:** novo `accessToken` + novo `Set-Cookie` (rotação).
- **400:** refresh ausente (`Refresh token ausente`) — `ResponseStatusException` mapeado para `ApiErrorResponse`.
- **401:** refresh inválido, expirado, ou inconsistência — `ApiErrorResponse`.
- **429:** mesmo bucket que login.

### `POST /api/v1/auth/logout`

- **Body:** nenhum.
- Revoga o refresh correspondente à cookie (se existir e for válido) e limpa a cookie.
- **204:** sem corpo; cliente deve **descartar** o access JWT localmente.

## Resto da API

- Pedidos autenticados: cabeçalho `Authorization: Bearer` seguido do access JWT.
- JWT inválido/expirado/malformado: filtro não autentica; pedidos a recursos protegidos recebem **401** JSON (`ApiErrorResponse`, mensagem `Não autenticado`) via `authenticationEntryPoint`.
- `@PreAuthorize` / roles: conforme controladores (ex. `USER`, `ADMIN`).

## Formato padrão de erro (`ApiErrorResponse`)

Usado pela maior parte dos erros de domínio e validação:

```json
{
  "message": "string",
  "code": "UNAUTHORIZED | VALIDATION_ERROR | ...",
  "status": 401,
  "path": "/api/v1/auth/login",
  "timestamp": "2026-04-26T12:00:00Z"
}
```

Códigos estáveis: ver enum `ApiErrorCode` no código.

## Respostas que **não** usam `ApiErrorResponse`

| Situação | HTTP | Corpo |
|----------|------|--------|
| Rate limit login/refresh/register | 429 | Texto plain (português) + `Retry-After` |
| `AuthCsrfGuardFilter`: POST refresh/logout com cookie de refresh e origem não permitida | 403 | Texto plain: origem não autorizada |

Para **refresh** e **logout** com cookie, o browser deve enviar **`Origin`** (ou `Referer` verificável) alinhado a `lojapp.cors.allowed-origins` (CSV).

## Rate limiting (auth)

- **Login + refresh:** até **60** pedidos por IP por minuto (modo `memory` por instância, ou `redis` se `LOJAPP_RATE_LIMIT_MODE=redis`).
- **Register:** por IP por hora — limite `lojapp.auth.registration.max-per-ip-per-hour` (default 10).
- IP: `ClientIpResolver` com `lojapp.security.trust-forward-headers` só **true** atrás de proxy de confiança.

## Revogação e política operacional

- **Logout:** apaga a linha de refresh correspondente ao token apresentado (cookie).
- **Novo login / registo / refresh bem-sucedido:** `issueTokens` chama `refreshTokens.deleteByUser_Id` — **uma sessão ativa por utilizador** no modelo actual (novo par substitui refresh anterior).
- **Refresh expirado:** linha removida; resposta 401.
- **Troca de palavra-passe / comprometimento:** qualquer fluxo futuro que altere credenciais deve **invalidar refreshes** do utilizador (padrão já suportado via `deleteByUser_Id` se reutilizado no serviço de utilizador).

## Auditoria

Eventos registados (ex.: `AUTH_LOGIN`, `AUTH_REFRESH`, `AUTH_LOGOUT`, `AUTH_REGISTER`) via `AuditService` — útil para suporte e revisão de segurança.

## Referência viva

- Swagger UI: `/swagger-ui.html` (tag **Auth**).
- Implementação: `AuthController`, `AuthService`, `AuthRefreshCookieSupport`, `JwtService`, `SecurityConfig`, `AuthCsrfGuardFilter`, `AuthRateLimitFilter`, `GlobalExceptionHandler`.
