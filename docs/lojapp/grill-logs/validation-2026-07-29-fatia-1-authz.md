# Validação — Fatia 1 authz (aliases JwtUser)

Data: 2026-07-29
Branch: `fix/authz-and-audit-slices`

## O que mudou

- `JwtUser.authorities()` emite **apenas** `ROLE_{papel}` (sem aliases bidirecionais / elevação `MANAGER→ADMIN`).
- `@PreAuthorize` nos controllers lista **explicitamente** os dois vocabulários onde o acesso é intencional.
- `hasRole('ADMIN')` em `/api/v1/users/admin/list` permanece exclusivo de ADMIN.
- `TestJwtAuth` deixa de conceder dual ROLE_USER+ROLE_CASHIER (etc.).
- Testes: `JwtUserTest` + `UserAuthorizationIntegrationTest.adminList_withManagerRole_returns403`.

## Verificação

- `./mvnw "-Dtest=JwtUserTest" test` — unitários de authorities.
- Integração `UserAuthorizationIntegrationTest` requer Docker/Testcontainers; neste ambiente Docker indisponível — marcar para revalidar no CI.

## Risco residual

- Controllers com listas longas de roles; unificação de enum no DB fica fora desta fatia.
- Cash session: REPRESENTATIVE **não** incluído (só CASHIER/MANAGER/USER/ADMIN).
