# Plano — Fase C piloto demo (Dia 11)

**Data:** 2026-07-23  
**Trilha Helder:** Normal (+ HITL local)  
**Ambiente:** Ubuntu/WSL local (API `:8081` + front `:5173`) — **não** Render

## Objetivo Dia 11

Conta local com dados fictícios (produtos + 1 NFe sintética) para o fluxo Dia 12.

## Aceite

| Critério | Como |
|----------|------|
| Conta local | Registo/login em `localhost` |
| Seed produtos | `scripts/seed-demo-roupas.sh` |
| NFe exemplo | `scripts/import-nfe-folder.sh` + fixtures `nfe-lote-sintetico-dia7` |
| Plano + evidência | Este ficheiro + grill-log |

## Fora de escopo

- Seed/NFe em produção Render  
- Fluxo ponta a ponta + screenshots (Dia 12)  
- Versionar senhas ou emails pessoais no Git  

## Ordem HITL (Ubuntu)

1. Postgres + `./mvnw spring-boot:run` (`.env` carregado)  
2. `npm run dev` em `frontend/`  
3. Registar conta local  
4. `seed-demo-roupas.sh` + `import-nfe-folder.sh`  

## Risco

Senha Postgres desalinhada do volume Docker → recriar com `docker compose down -v` ou alinhar `.env`.
