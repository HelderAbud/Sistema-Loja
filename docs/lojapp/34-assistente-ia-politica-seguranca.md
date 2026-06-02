# Assistente / agentes — item 3: política de segurança

**Status:** política operacional para automação credenciada (fatia C).  
**Alinhado a:** [12-contratos-autenticacao-e-sessao.md](./12-contratos-autenticacao-e-sessao.md), [13-threat-model-auth-spa.md](./13-threat-model-auth-spa.md), `AGENTS.md` (gate humano, sem segredos no git).

## 1. Princípios

| Princípio | Significado para o assistente |
|-----------|-------------------------------|
| **JWT só em ambiente controlado** | Tokens de acesso e refresh **nunca** em repositório, screenshots públicos, issues ou prompts partilhados. Usar máquina/CI privada e variáveis de ambiente ou secret store. |
| **Segredos só em ambiente** | `LOJAPP_JWT_SECRET`, palavras-passe de BD, `LOJAPP_REGISTRATION_INVITE_SECRET`, chaves S3/NFe, **e** chaves de API de LLM ficam em env / gestor de segredos — não em ficheiros versionados. |
| **API como fronteira** | O agente não contorna a API nem executa SQL direto; isolamento continua a ser o `user_id` do token (e papéis Spring). |
| **Mutação com gate humano** | Conforme [32-assistente-ia-fatia-vertical-v1.md](./32-assistente-ia-fatia-vertical-v1.md): corpo visível + aprovação antes de `POST`/`PUT` relevantes. |

## 2. Credenciais LojApp

| Segredo / config | Variável (referência) | Notas para automação |
|------------------|----------------------|----------------------|
| Assinatura JWT | `LOJAPP_JWT_SECRET` | Obrigatório em arranque; mínimo 32 bytes. Quem corre a **API** precisa disto; quem só **chama** a API precisa de um **access token válido**, não do segredo. |
| TTL access | `LOJAPP_JWT_EXPIRATION_MS` | Default ~15 min — scripts longos devem planear **refresh** (ver doc 12) ou novo login, sem gravar tokens em logs. |
| Refresh | Cookie `lojapp_rt` ou body | Automação fora do browser: preferir fluxo documentado em 12 (refresh com body) em canal seguro; não commitar refresh tokens. |
| Base de dados | `SPRING_DATASOURCE_*` | Só o **processo da API**; o agente **não** deve usar credenciais de BD salvo exceção operacional explícita e isolada (não é o desenho da fatia C). |
| Registo / convite | `LOJAPP_REGISTRATION_INVITE_SECRET` | Não expor em demos de agente; registo em produção deve permanecer política fechada. |

## 3. Tokens usados pelo “ator” (script, Cursor, n8n, etc.)

- **Preferir** utilizador técnico ou conta de **menor privilégio** que ainda permita a fatia (ex.: não usar `ADMIN` se `USER` chega).
- **PDV:** papéis `CASHIER`, `SELLER`, `MANAGER` — tokens diferentes dos de backoffice; não misturar no mesmo roteiro sem consciência de escopo (ver [33-assistente-ia-mapeamento-api-fatia-c.md](./33-assistente-ia-mapeamento-api-fatia-c.md)).
- **Não** incluir `Authorization: Bearer` em traces públicos, relatórios de erro ou histórico de chat exportado.
- **Idempotency-Key** em mutações: valor opaco gerado por execução (UUID); não reutilizar entre **corpos diferentes**.

## 4. Provedores de LLM (se aplicável)

| Risco | Mitigação mínima |
|-------|------------------|
| Dados pessoais ou comerciais no prompt | Minimizar PII; preferir IDs e totais agregados; política de retenção do fornecedor revista. |
| Exfiltração de JWT no prompt | Nunca colar token completo em ferramentas cloud sem política enterprise; preferir chamadas **locais** à API com token em env. |
| Chave de API do modelo | Só env; rotação se vazar; não partilhar entre projetos públicos. |

## 5. Rede e ambientes

- **Produção:** HTTPS; cookies de refresh com `Secure` conforme config (ver 12).
- **Desenvolvimento local:** ainda assim tratar JWT como sensível; `localhost` não é permissão para commitar segredos.
- **Rate limit:** login e auth estão limitados por IP — automação não deve martelar `POST /api/v1/auth/login`; preferir refresh ou token pré-obtido com TTL gerido.

## 6. Checklist de conformidade (item 3)

Use antes de correr qualquer demo ou pipeline com agente:

- [ ] Nenhum segredo LojApp nem chave LLM está em git (incl. `.env` acidental — ver `.gitignore` e histórico).
- [ ] JWT / refresh usados só em memória ou secret store da sessão de execução.
- [ ] Ambiente (dev/stage/prod) explícito; não apontar automação a produção sem revisão.
- [ ] Papéis do token alinhados à fatia (backoffice vs PDV).
- [ ] Plano de mutações inclui gate humano e `Idempotency-Key` onde a API suporta.
- [ ] Logs e capturas de ecrã sem cabeçalhos `Authorization` nem corpos com palavras-passe.

## 7. Próximo item sugerido

Inventário operacional fino (DTOs exemplo, roteiro de chamadas) + escolha explícita do **ator** (ferramenta única), depois protótipo.
