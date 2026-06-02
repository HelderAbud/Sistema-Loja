# Definition of Done (DoD) único — backend + frontend

Objetivo: eliminar duplicidade entre checklist de PR e checklist de aceite, mantendo um único critério operacional de "pronto para entrega".

## 1) Escopo e qualidade do change

- [ ] Objetivo da entrega está claro em 1 frase.
- [ ] Diff está focado (sem escopo colateral desnecessário).
- [ ] Sem artefatos locais versionados (`target/`, `node_modules/`, reports temporários, backups locais).

## 2) Arquitetura e contrato

- [ ] Contratos públicos preservados ou versionados (seguir `17-versionamento-api-rest.md`).
- [ ] Endpoints REST em `/api/v1` (mudança breaking exige plano de nova versão).
- [ ] DTOs/validações e tratamento de erro alinhados ao padrão global.
- [ ] Se houve mudança de schema: nova migration Flyway (sem alterar migrações já aplicadas).

## 3) Segurança e operação

- [ ] Sem segredos no commit.
- [ ] Regras de autenticação/sessão/cookies/CORS revisadas quando houver impacto em auth.
- [ ] Mudança operacional relevante registrada nos docs/runbooks.

## 4) Verificação obrigatória

- [ ] Backend: testes relevantes verdes (`mvn test` ou perfil equivalente no contexto).
- [ ] Frontend (quando aplicável): `npm run lint`, `npm run test`, `npm run e2e` conforme impacto.
- [ ] CI verde nos jobs obrigatórios (`backend-unit`, `backend-integration`, `frontend`, `docker-image`, `security-fs`).

## 5) Evidência e rastreabilidade

- [ ] Evidência objetiva registrada (comando + resultado, relatório, log ou captura).
- [ ] Decisão técnica não trivial registrada em `18-decisoes-e-checklist-entrega.md`.
- [ ] Documentação afetada atualizada (arquitetura, operação, API, ou checklist correspondente).

## 6) Critério final

- [ ] Só considerar "concluído" quando todos os itens aplicáveis estiverem marcados.
