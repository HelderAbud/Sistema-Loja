# Validation — Trilha Dia 8 (Vercel + CORS)

**Data:** 2026-07-20  
**Triagem:** Helder Normal + HITL  
**Provider front:** Vercel · **API:** Render  

## Checklist Dia 8

| Item | Resultado |
|------|-----------|
| Deploy frontend | OK — `https://sistema-loja-psi.vercel.app` |
| `VITE_API_BASE` → API Render | OK — `https://lojapp-api.onrender.com` |
| CORS na API | OK — origem Vercel em `LOJAPP_CORS_ORIGINS` |
| Smoke login → dashboard | OK — Helder confirmou “consegui conectar” |

## URLs (públicas)

| Serviço | URL |
|---------|-----|
| Front | https://sistema-loja-psi.vercel.app |
| API health | https://lojapp-api.onrender.com/actuator/health |

## Notas / residual

- Registo público foi ligado temporariamente para criar conta; **recomendado** voltar `LOJAPP_REGISTRATION_ENABLED=false` após o smoke.
- Cookie refresh `SameSite=Lax` cross-origin: F5 pode pedir login outra vez — aceitável no MVP; endurecer no Dia 9 se necessário.
- Auto-Deploy Docker no Render: preferir **off** para commits só de docs (evitar timeout).

## Próximo

Dia 9 — smoke + segurança + URLs no README.

## Aprovado?

- [x] Fatia Dia 8 verificável (front + login)
- [ ] Commit/PR docs — aguarda `PR LojApp`
- [ ] Fechar registo público (HITL Render)
