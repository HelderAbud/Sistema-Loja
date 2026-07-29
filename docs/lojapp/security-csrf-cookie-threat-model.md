# CSRF / cookie — modelo de ameaça (LojApp)

Data: 2026-07-29

## Modelo atual

- Spring Security **CSRF token clássico desligado** (`csrf.disable()`): API pensada para JWT Bearer + refresh em cookie HttpOnly.
- Mitigação: [`AuthCsrfGuardFilter`](../../src/main/java/com/lojapp/security/AuthCsrfGuardFilter.java) exige `Origin`/`Referer` permitidos em **qualquer POST** sob `/api/v1/auth/**` **quando** o cookie de refresh está presente.

## O que isto cobre

- CSRF clássico de browser em `refresh` / `logout` (e login/register se já existir cookie de sessão).

## Residual

- Cliente sem `Origin`/`Referer` (alguns clients nativos) — não bloqueado se o cookie existir e headers faltarem sem allow.
- CSRF em mutações de negócio autenticadas só com Bearer no header (sem cookie) — risco baixo de CSRF browser clássico.
- Preferência em produção: cookie `SameSite=Lax/Strict` + CORS estrito + HTTPS.

## Verificação

- `AuthCsrfGuardFilterTest` (inclui login com cookie + origem má).
