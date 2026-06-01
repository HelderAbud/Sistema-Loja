# Grill — A2 (segredos fora do Git + rotação JWT)

**Data:** 2026-05-24 (execução 2026-06-01)
**Participantes:** utilizador + agente
**Plano:** `plano-consolidado-melhorias-2026-05-24.md`
**Branch:** `fix/A2-secrets-jwt-rotation`

## Escopo desta sessão

Garantir que nenhum segredo vive no working tree, documentar geração de `LOJAPP_JWT_SECRET` por ambiente e registar a rotação (decisão **S** do grill de Fase 0). Repositório já público.

## Achados na exploração

| # | Achado | Ação |
|---|--------|------|
| 1 | `docker-compose.yml` já usa `${POSTGRES_PASSWORD:?}` e `${LOJAPP_JWT_SECRET:?}` (sem literais) | Mantido |
| 2 | **Histórico Git expõe** `LOJAPP_JWT_SECRET: SegredoSuperForteComMaisDe32Bytes123456789` (commit antigo do compose) | Segredo **queimado**; rotação obrigatória; nota no README e `.env.example` |
| 3 | `.gitignore` linha 18 com bug: `**/application-local.properties.idea/` (concatenado) → `.idea/` não era ignorado | Corrigido: linhas separadas + bloco `# IDE` com `.idea/` |
| 4 | `.idea/` não estava tracked (só faltava ignorar) | Sem necessidade de `git rm` |
| 5 | `LOJAPP_REGISTRATION_ENABLED: "true"` no compose dev | Intencional (dev); documentado por comentário no compose |

## Perguntas respondidas

| # | Pergunta | Resposta acordada |
|---|----------|-------------------|
| 1 | Rotação JWT obrigatória? | **S** (Fase 0) — segredo novo por ambiente; valor antigo do histórico nunca reutilizado |
| 2 | Reescrever histórico Git para remover segredo? | **Não** — destrutivo em repo público; rotação neutraliza o valor exposto |
| 3 | `REGISTRATION_ENABLED=true` no dev é intencional? | Sim, só dev; produção mantém `false` (ver `.env.example` / compose prod) |

## Alterações (código/docs)

- `.gitignore`: separa `application-local.properties` de `.idea/`; adiciona bloco `# IDE`.
- `.env.example`: nota de que o segredo de exemplo foi exposto e deve gerar-se um novo.
- `README.md`: secção de geração de `LOJAPP_JWT_SECRET` por ambiente (openssl / PowerShell) + aviso de re-login após rotação.

## Validação (gate do utilizador — pendente)

Comandos a correr localmente (Docker não disponível no ambiente do agente):

```powershell
# 1. Compose deve FALHAR sem .env (variáveis obrigatórias :?)
Rename-Item .env .env.bak
docker compose config            # deve reclamar de POSTGRES_PASSWORD / LOJAPP_JWT_SECRET
Rename-Item .env.bak .env

# 2. Gerar segredo NOVO e colocar em .env (LOJAPP_JWT_SECRET=...)
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 | % {[char]$_})

# 3. Subir e validar
docker compose up -d
curl http://localhost:8000/actuator/health   # status UP

# 4. .env nao tracked
git status --ignored | findstr .env
```

## DoD A2

- [x] `.env.example` sem segredos reais
- [x] `.gitignore` corrigido (.env, .idea/, overrides locais)
- [x] README documenta geração + rotação do JWT
- [x] Decisão sobre `REGISTRATION_ENABLED` documentada
- [ ] `docker compose up` validado com `.env` (gate utilizador)
- [ ] `LOJAPP_JWT_SECRET` rotacionado no ambiente local (gate utilizador)
- [ ] CHECKLIST 4.1 + Passo 5 marcados (após validação)

## Aprovado para fechar A2?

- [ ] Sim — após validação local (compose + rotação)
