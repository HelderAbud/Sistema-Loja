# Checklist — Segurança operacional mínima (Dia 8)

Objetivo: confirmar que **segredos**, **CORS**, **política de sessão** (JWT + refresh) e **respostas 401** estão alinhados com publicação controlada (demo ou produção inicial).  
Referências: [12-contratos-autenticacao-e-sessao.md](./12-contratos-autenticacao-e-sessao.md), [13-threat-model-auth-spa.md](./13-threat-model-auth-spa.md), `.env.example`, `application.yml`, `docker-compose.prod.yml`.

## 1. Variáveis de ambiente sensíveis

| Variável | Uso | Dev local | Demo / produção inicial |
|----------|-----|-----------|-------------------------|
| `LOJAPP_JWT_SECRET` | Assinatura HS256 do access JWT | Obrigatório (≥ ~32 caracteres; validado ao arranque) | **Obrigatório**; valor aleatório forte; **nunca** commitar |
| `SPRING_DATASOURCE_PASSWORD` / `POSTGRES_PASSWORD` | Ligação à BD | Definir mesmo valor em compose/prod | **Obrigatório**; distinto do default `lojapp_local_dev_only` |
| `LOJAPP_CORS_ORIGINS` | Origens permitidas (browser → API) | Default inclui `localhost:3000` etc. | Lista **explícita** das URLs do frontend (CSV, sem espaços) |
| `LOJAPP_NFE_STORAGE_S3_*` | XML em S3/MinIO | Opcional se `LOJAPP_NFE_STORAGE_BACKEND=database` | Preencher se `backend=s3`; credenciais só por env/secret manager |
| `LOJAPP_REGISTRATION_INVITE_SECRET` | Registo público com convite | Vazio = sem convite forçado | Se registo aberto: definir e partilhar convite por canal seguro |
| `LOJAPP_ACTUATOR_METRICS_ANONYMOUS` | Métricas sem JWT | `false` (default seguro) | Manter **`false`** salvo Prometheus autenticado atrás do proxy |
| `LOJAPP_TRUST_FORWARD_HEADERS` | Confiança em `X-Forwarded-For` | `false` | `true` **só** atrás de proxy que sanitiza cabeçalhos |
| `LOJAPP_RATE_LIMIT_MODE` | Limite login/refresh | `memory` OK em instância única | `redis` em `docker-compose.prod` (réplicas / consistência) |

**Ação:** rever `.env` / secrets do ambiente alvo e marcar cada linha como preenchida ou N/A.

## 2. Política de sessão (demo vs produção inicial)

| Aspeto | Configuração | Notas |
|--------|----------------|-------|
| Access JWT | `LOJAPP_JWT_EXPIRATION_MS` (default ~15 min) | Janela curta reduz impacto de vazamento do token em memória da SPA |
| Refresh | `LOJAPP_JWT_REFRESH_EXPIRATION_MS` (default ~14 dias) + cookie `lojapp_rt` | Rotação a cada refresh; reutilização do refresh gasta → erro |
| Cookie Secure | `lojapp.auth.refresh-cookie-secure` / `LOJAPP_AUTH_REFRESH_COOKIE_SECURE` | **HTTPS (demo/prod): `true`**. HTTP local: `false` |
| SameSite / HttpOnly | Definidos no código | HttpOnly + SameSite=Lax — ver doc 12 |
| Estado servidor | `SessionCreationPolicy.STATELESS` | Sem sessão HTTP clássica; estado em JWT + refresh persistido |

**Demo pública:** alinhar TTLs com risco aceite; não aumentar `LOJAPP_JWT_EXPIRATION_MS` sem motivo documentado.

**Produção:** o perfil `prod` em `application-prod.yml` já define `lojapp.auth.refresh-cookie-secure: true`. O serviço `api` em `docker-compose.prod.yml` expõe `LOJAPP_AUTH_REFRESH_COOKIE_SECURE` (default `true` no compose) para override explícito; use `false` apenas em teste interno sem TLS.

## 3. Comportamento de auth em erro (token inválido / expirado / ausente)

Comportamento esperado (código):

- **Sem `Authorization` em recurso protegido:** `401` + JSON `ApiErrorResponse` (mensagem *Não autenticado*, `code=UNAUTHORIZED`) via `authenticationEntryPoint` em `SecurityConfig`.
- **Bearer presente mas JWT inválido, malformado, assinatura errada ou expirado:** `JwtAuthFilter` não autentica; mesmo `401` JSON ao aceder a recurso protegido.
- **Testes unitários:** `JwtServiceTest` cobre expirado, assinatura errada, malformado e `sub` não numérico.

Verificação rápida (API a correr):

```powershell
.\scripts\verify-auth-errors.ps1
```

Ou manualmente:

1. `curl -i` em `GET /api/v1/lojapp/products` sem cabeçalho → `401`.
2. Idem com `Authorization: Bearer not-a-jwt` → `401`.
3. Opcional: JWT válido alterado no último carácter → `401`.

## 4. Registo de revisão (preencher na execução do Dia 8)

| Item | OK? | Evidência (data / nota) |
|------|-----|-------------------------|
| Segredos revistos (JWT, DB, S3 se aplicável) | ☐ | |
| `LOJAPP_CORS_ORIGINS` alinhado ao frontend deployado | ☐ | |
| Política de cookie refresh documentada para o ambiente | ☐ | |
| `verify-auth-errors.ps1` (ou equivalente manual) executado | ☐ | |
| Pendências aceites ou encaminhadas para `20-backlog-seguranca-residual.md` | ☐ | |

## 5. Relação com backlog residual

Itens **não** exigidos para fechar o Dia 8 (melhoria contínua): ver [20-backlog-seguranca-residual.md](./20-backlog-seguranca-residual.md) (ex.: reuse de refresh, MFA, ZAP em CI).
