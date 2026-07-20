# Runbook HITL — Dia 8 (Vercel + CORS)

## A) Vercel — criar o front

1. [vercel.com](https://vercel.com) → login com GitHub
2. **Add New** → **Project** → `HelderAbud/Sistema-Loja`
3. Configure:
   - **Root Directory:** `frontend` (Edit → select)
   - **Framework Preset:** Vite
   - **Build Command:** `npm run build` (default)
   - **Output Directory:** `dist`
4. **Environment Variables** (Production):

| Name | Value |
|------|--------|
| `VITE_API_BASE` | `https://lojapp-api.onrender.com` |
| `VITE_CSP_CONNECT_SRC` | `https://lojapp-api.onrender.com` |

5. **Deploy** → espera Ready
6. Copia a URL: `https://…..vercel.app`

## B) Render — CORS

1. `lojapp-api` → **Environment**
2. Edita `LOJAPP_CORS_ORIGINS` para incluir a URL do Vercel **exacta**, ex.:

```text
https://lojapp-xxxxx.vercel.app,http://localhost:3000
```

Sem barra no fim. `https` obrigatório.

3. **Save** (reinicia o serviço — **não** precisa rebuild Docker completo se só mudou env)

## C) Smoke

1. Abre o URL do Vercel
2. Login (ou registo, se `LOJAPP_REGISTRATION_ENABLED=true` na API)
3. Confirma dashboard / uma tela autenticada

Se CORS falhar: DevTools → Network → pedido vermelho / erro CORS → confere origem exacta na var.

## D) Reportar no chat

```text
Dia 8 OK — front: https://….vercel.app
API: https://lojapp-api.onrender.com
smoke: login → dashboard OK
```
