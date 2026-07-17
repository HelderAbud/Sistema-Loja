# Runbook HITL — Dia 7 (API + Postgres)

Segue **um** provider. Não partilhes secrets no chat.

## A) Railway (recomendado se já usas GitHub)

1. Conta em [railway.app](https://railway.app) → New Project → **Deploy from GitHub** → `HelderAbud/Sistema-Loja` → branch `Principal`.
2. Add service → **PostgreSQL**.
3. No serviço da API: Settings → Root Directory `/` → Dockerfile path `Dockerfile`.
4. Variables da API (Variables):
   - `SPRING_PROFILES_ACTIVE=prod`
   - `LOJAPP_JWT_SECRET` = valor do teu `.env`
   - `LOJAPP_RATE_LIMIT_MODE=memory`
   - `LOJAPP_CORS_ORIGINS=http://localhost:3000`
   - `LOJAPP_TRUST_FORWARD_HEADERS=true`
   - `SERVER_PORT=8000` (se o health falhar por porta, alinha com a `PORT` que o Railway mostrar)
5. Ligar Postgres: em Variables, referência às vars do plugin Postgres **ou** monta:
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:PORT/DB`
   - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
   (Railway costuma expor `DATABASE_URL` — converte para JDBC se necessário: `postgres://` → `jdbc:postgresql://`)
6. Generate Domain no serviço API.
7. Teste: `https://<domínio>/actuator/health`

## B) Render

1. [render.com](https://render.com) → New → **PostgreSQL** (free).
2. New → **Web Service** → repo GitHub → Runtime **Docker** → Dockerfile na raiz.
3. Mesmas variáveis da tabela do plano (`docs/lojapp/plans/plan-2026-07-17-deploy-api-dia-7.md`).
4. Health check path: `/actuator/health`
5. Deploy → abrir URL `/actuator/health`

## Depois do health UP

Responde no chat (sem secrets):

```text
Dia 7 OK — provider: Railway|Render
URL: https://....
health: UP
```

Aí marco a trilha + grill-log e seguimos Dia 8 (frontend).
