# Checklist de handoff técnico (novo colaborador / junior)

## 1) Preparação local

- [ ] Instalar Java 21, Maven, Node 20+ e Docker Desktop.
- [ ] Configurar variáveis mínimas (`LOJAPP_JWT_SECRET`, credenciais DB, CORS quando necessário).
- [ ] Subir stack local (`docker compose up -d`) e validar `GET /actuator/health`.

## 2) Leitura mínima obrigatória

- [ ] `28-indice-tecnico-unificado.md` (visão de navegação).
- [ ] `27-definition-of-done-unico.md` (critério de entrega).
- [ ] `11-checklist-pr-e-convencoes-repositorio.md` (higiene de PR).
- [ ] `12-contratos-autenticacao-e-sessao.md` (auth/sessão).
- [ ] `13-estoque-concorrencia-e-idempotencia.md` (regras críticas).

## 3) Fluxo de contribuição

- [ ] Criar branch curta e escopo pequeno por tarefa.
- [ ] Rodar verificações locais relevantes (`mvn test`, `npm run lint`, `npm run test`, `npm run e2e` quando aplicável).
- [ ] Registrar evidência no PR e/ou documentação técnica associada.
- [ ] Só concluir com DoD único cumprido.

## 4) Segurança e operação

- [ ] Não commitar segredos nem artefatos locais.
- [ ] Validar impacto de mudanças em auth/cookies/CORS e em migrations.
- [ ] Em mudanças operacionais, atualizar runbook/checklist correspondente.

## 5) Critério de autonomia inicial

- [ ] Consegue abrir PR pequeno passando CI sem ajustes de emergência.
- [ ] Consegue explicar o fluxo de venda, NFe e dashboard com base nos docs.
- [ ] Consegue executar e validar checklist de deploy em ambiente de teste.
