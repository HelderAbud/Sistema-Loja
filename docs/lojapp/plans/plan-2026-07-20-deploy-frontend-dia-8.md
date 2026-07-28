# Plano Dia 8 — Frontend Vercel + CORS

**Data:** 2026-07-20  
**Trilha Helder:** Normal (+ HITL)  
**Pré-requisito:** API Render health UP (`https://lojapp-api.onrender.com`)

## Objetivo

SPA no ar (Vercel) a falar com a API; smoke login → dashboard.

## Aceite

| Critério | Verificação |
|----------|-------------|
| Front público HTTPS | URL `*.vercel.app` abre |
| Build com API | `VITE_API_BASE` + CSP `VITE_CSP_CONNECT_SRC` |
| CORS na API | `LOJAPP_CORS_ORIGINS` = origem exacta do Vercel (https, sem barra final) |
| Smoke | login → dashboard (ou registo se `LOJAPP_REGISTRATION_ENABLED=true`) |

## Env Vercel (Build)

| Key | Value |
|-----|--------|
| `VITE_API_BASE` | `https://lojapp-api.onrender.com` |
| `VITE_CSP_CONNECT_SRC` | `https://lojapp-api.onrender.com` |

Root Directory: `frontend`  
Framework: Vite  
Build: `npm run build`  
Output: `dist`

## Env Render (API) — depois de ter a URL do front

| Key | Value |
|-----|--------|
| `LOJAPP_CORS_ORIGINS` | `https://SEU-PROJETO.vercel.app` (exacto; pode manter `,http://localhost:5173` para dev) |

**Não** redeploy Docker só por docs; só **Save** nas vars da API (restart leve) ou Manual Deploy se necessário.

## Ordem HITL

1. Criar projeto Vercel (GitHub `Sistema-Loja`, root `frontend`)
2. Definir env de build → Deploy
3. Copiar URL `https://….vercel.app`
4. Atualizar CORS no Render
5. Smoke login → dashboard
6. Reportar URLs (sem secrets)

## Risco cookie cross-site

Front (Vercel) e API (Render) são **origens diferentes**. Refresh em cookie `SameSite=Lax` pode **não** ir no `fetch` cross-site.  
Smoke mínimo: login devolve accessToken e dashboard carrega **na mesma sessão** (token em memória).  
Se F5 perder sessão: documentar residual Dia 9 / possível `SameSite=None` (HITL futuro).

## Fora de escopo

- README Demo URLs (Dia 9)
- Etapa 6 fechada (Dia 10)
