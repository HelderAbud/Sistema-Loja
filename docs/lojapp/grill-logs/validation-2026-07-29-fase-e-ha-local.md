# Validation — Fase E (HA local · Dias 19–21)

**Data:** 2026-07-29  
**Triagem:** Helder Simple · fast path  

## Checklist

| Item | Resultado |
|------|-----------|
| `docs/portfolio/etapa-ha-local.md` | OK — 2 APIs + nginx + curl; honestidade “não é AWS ALB” |
| Compose override / exemplo | OK — `docker-compose.ha.yml` + `deploy/ha/nginx.conf` |
| Script curl loop | OK — `scripts/ha-curl-loop.sh` |
| Link em etapas | OK — ponto 5 em “Como ler isto numa entrevista” |
| Trilha marcada | OK |

## Verificação documental

- Porta lab **8088** (não colide com API canónica 8081)
- Rate limit **redis** no exemplo (adequado a multi-réplica)
- Disclaimer explícito: lab local ≠ AWS ALB/ECS/EKS

## Não executado nesta fatia

- `docker compose -f docker-compose.ha.yml up` (build pesado; smoke opcional do Helder)
- Publicação LinkedIn / commit (HITL)

## Riscos residuais

- Stack HA consome RAM (2 JVMs); free tier cloud continua single-instance
- Script bash pensado para WSL/Linux; no Windows puro usar `wsl -e bash scripts/ha-curl-loop.sh`
